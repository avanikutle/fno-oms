package com.fnooms.broker.mstock.api;

import com.fnooms.broker.BrokerException;
import com.fnooms.broker.mstock.MStockConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Core client managing HTTP communication, authentication, and JSON helpers for mStock APIs.
 */
public class MStockCoreClient {

    private static final Logger log = LoggerFactory.getLogger(MStockCoreClient.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final MStockConfig config;
    private final OkHttpClient http;

    public MStockCoreClient(MStockConfig config, OkHttpClient http) {
        this.config = config;
        this.http = http;
    }

    public String getBaseUrl() {
        return config.getBaseUrl();
    }

    public JsonObject executeGet(String url) throws BrokerException {
        return execute(buildRequest(url, null, "GET"));
    }

    public JsonObject executePost(String url, String jsonBody) throws BrokerException {
        return execute(buildRequest(url, jsonBody, "POST"));
    }

    public JsonObject executePut(String url, String jsonBody) throws BrokerException {
        return execute(buildRequest(url, jsonBody, "PUT"));
    }

    public JsonObject executeDelete(String url, String jsonBody) throws BrokerException {
        return execute(buildRequest(url, jsonBody, "DELETE"));
    }

    private JsonObject execute(Request request) throws BrokerException {
        long start = System.currentTimeMillis();
        try (Response resp = http.newCall(request).execute()) {
            String bodyStr = resp.body() != null ? resp.body().string() : "{}";
            long elapsed = System.currentTimeMillis() - start;
            log.debug("{} {} → {} ({}ms)", request.method(), request.url(), resp.code(), elapsed);

            JsonObject json = new JsonObject();
            try {
                JsonElement root = JsonParser.parseString(bodyStr);
                if (root.isJsonArray()) {
                    JsonArray arr = root.getAsJsonArray();
                    if (arr.size() > 0) json = arr.get(0).getAsJsonObject();
                } else if (root.isJsonObject()) {
                    json = root.getAsJsonObject();
                }
            } catch (Exception e) {
                // Ignore parse errors, fallback to raw body string for error reporting
            }

            if (!resp.isSuccessful()) {
                String errorMsg = safeString(json, "message");
                if (errorMsg == null) errorMsg = bodyStr;
                throw new BrokerException(
                        "mStock API error " + resp.code() + ": " + errorMsg,
                        resp.code(), null);
            }

            if (json.has("status") && !json.get("status").isJsonNull()) {
                JsonElement statusEl = json.get("status");
                boolean ok = false;
                if (statusEl.isJsonPrimitive()) {
                    if (statusEl.getAsJsonPrimitive().isBoolean()) {
                        ok = statusEl.getAsBoolean();
                    } else if (statusEl.getAsJsonPrimitive().isString()) {
                        ok = "success".equalsIgnoreCase(statusEl.getAsString());
                    }
                }
                if (!ok) {
                    String msg = safeString(json, "message");
                    throw new BrokerException("mStock returned status=" + statusEl.getAsString() + ": " + msg, resp.code(), null);
                }
            }
            return json;
        } catch (IOException e) {
            throw new BrokerException("Network error calling mStock API: " + e.getMessage(), e);
        }
    }

    private Request buildRequest(String url, String jsonBody, String method) {
        String authHeader = config.getAuthorizationHeader();
        String apiVersion = config.getApiVersion();
        String apiKey = config.getPrivateKey();

        log.info("MSTOCK DEBUG - URL: {}", url);
        log.info("MSTOCK DEBUG - Auth Header: {}", authHeader);
        log.info("MSTOCK DEBUG - X-Mirae-Version: {}", apiVersion);

        Request.Builder builder = new Request.Builder()
                .url(url)
                .header("Authorization", authHeader)
                .header("X-PrivateKey", apiKey)
                .header("X-Mirae-Version", apiVersion)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");

        switch (method) {
            case "GET" -> builder.get();
            case "POST" -> builder.post(RequestBody.create(jsonBody != null ? jsonBody : "{}", JSON));
            case "PUT" -> builder.put(RequestBody.create(jsonBody != null ? jsonBody : "{}", JSON));
            case "DELETE" -> builder.delete(jsonBody != null ? RequestBody.create(jsonBody, JSON) : null);
            default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }
        return builder.build();
    }

    // --- JSON Helpers for APIs to use ---

    public String safeString(JsonObject o, String key) {
        return (o.has(key) && !o.get(key).isJsonNull()) ? o.get(key).getAsString() : null;
    }

    public BigDecimal safeDecimal(JsonObject o, String key) {
        try {
            return (o.has(key) && !o.get(key).isJsonNull()) ? o.get(key).getAsBigDecimal() : null;
        } catch (Exception e) { return null; }
    }

    public long safeLong(JsonObject o, String key) {
        try {
            return (o.has(key) && !o.get(key).isJsonNull()) ? o.get(key).getAsLong() : 0L;
        } catch (Exception e) { return 0L; }
    }

    public int safeInt(JsonObject o, String key) {
        try {
            return (o.has(key) && !o.get(key).isJsonNull()) ? o.get(key).getAsInt() : 0;
        } catch (Exception e) { return 0; }
    }

    public boolean safeBoolean(JsonObject o, String key) {
        return (o.has(key) && !o.get(key).isJsonNull()) && o.get(key).getAsBoolean();
    }

    public JsonObject safeGetObject(JsonObject o, String key) {
        return (o.has(key) && o.get(key).isJsonObject()) ? o.getAsJsonObject(key) : null;
    }

    public JsonArray safeGetArray(JsonObject o, String key) {
        return (o.has(key) && o.get(key).isJsonArray()) ? o.getAsJsonArray(key) : null;
    }
}
