package com.fnooms.util;

import com.google.gson.*;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.time.Instant;

/**
 * JSON serialization utilities using Gson.
 * Includes a TypeAdapter for java.time.Instant (needed on Java 25 due to
 * module encapsulation — Gson cannot reflectively access java.time internals).
 */
public class JsonUtil {

    /** TypeAdapter: Instant ↔ ISO-8601 string (e.g. "2026-07-12T00:00:00Z") */
    private static final TypeAdapter<Instant> INSTANT_ADAPTER = new TypeAdapter<>() {
        @Override public void write(JsonWriter out, Instant value) throws IOException {
            if (value == null) { out.nullValue(); return; }
            out.value(value.toString());
        }
        @Override public Instant read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) { in.nextNull(); return null; }
            return Instant.parse(in.nextString());
        }
    };

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
            .registerTypeAdapter(Instant.class, INSTANT_ADAPTER)
            .create();

    private static final Gson GSON_COMPACT = new GsonBuilder()
            .serializeNulls()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
            .registerTypeAdapter(Instant.class, INSTANT_ADAPTER)
            .create();

    private JsonUtil() {}

    public static String toJson(Object obj) {
        return GSON.toJson(obj);
    }

    public static String toJsonCompact(Object obj) {
        return GSON_COMPACT.toJson(obj);
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }

    public static JsonObject parseObject(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    public static JsonArray parseArray(String json) {
        return JsonParser.parseString(json).getAsJsonArray();
    }

    /**
     * Write a JSON response to an HttpServletResponse.
     */
    public static void writeJson(HttpServletResponse resp, int status, Object payload) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json;charset=UTF-8");
        resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        try (PrintWriter pw = resp.getWriter()) {
            pw.print(GSON_COMPACT.toJson(payload));
        }
    }

    /**
     * Build a standard success envelope.
     */
    public static JsonObject success(Object data) {
        JsonObject obj = new JsonObject();
        obj.addProperty("status", "success");
        obj.add("data", GSON_COMPACT.toJsonTree(data));
        return obj;
    }

    /**
     * Build a standard error envelope.
     */
    public static JsonObject error(String message) {
        JsonObject obj = new JsonObject();
        obj.addProperty("status", "error");
        obj.addProperty("message", message);
        return obj;
    }
}
