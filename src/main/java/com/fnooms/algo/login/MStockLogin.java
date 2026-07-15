package com.fnooms.algo.login;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class MStockLogin implements BrokerLogin {

    private static final String LOGIN_URL = "https://api.mstock.trade/openapi/typeb/connect/login";
    private static final String VERIFY_TOTP_URL = "https://api.mstock.trade/openapi/typeb/session/verifytotp";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient http = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();

    @Override
    public String login(String userId, String password, String totp) throws Exception {
        throw new UnsupportedOperationException("Needs API Key for verifytotp");
    }

    public String login(String userId, String password, String totp, String apiKey) throws Exception {
        // Step 1: Login
        String loginJson = String.format("{\"clientcode\":\"%s\",\"password\":\"%s\",\"totp\":\"\",\"state\":\"\"}", userId, password);
        RequestBody loginBody = RequestBody.create(loginJson, JSON);

        Request loginRequest = new Request.Builder()
                .url(LOGIN_URL)
                .post(loginBody)
                .header("X-Mirae-Version", "1")
                .header("Content-Type", "application/json")
                .build();

        try (Response response = http.newCall(loginRequest).execute()) {
            String respStr = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new Exception("mStock Login failed: " + response.code() + " - " + respStr);
            }
            System.out.println("Login Step 1 successful.");
        }

        // Step 2: Verify TOTP
        String totpJson = String.format("{\"api_key\":\"%s\",\"totp\":\"%s\"}", apiKey, totp);
        RequestBody totpBody = RequestBody.create(totpJson, JSON);

        Request totpRequest = new Request.Builder()
                .url(VERIFY_TOTP_URL)
                .post(totpBody)
                .header("X-Mirae-Version", "1")
                .header("Content-Type", "application/json")
                .build();

        try (Response response = http.newCall(totpRequest).execute()) {
            String respStr = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new Exception("mStock Verify TOTP failed: " + response.code() + " - " + respStr);
            }
            
            JsonObject json = JsonParser.parseString(respStr).getAsJsonObject();
            JsonElement dataEl = json.get("data");
            if (dataEl != null && dataEl.isJsonObject()) {
                JsonObject data = dataEl.getAsJsonObject();
                if (data.has("access_token")) {
                    return data.get("access_token").getAsString();
                }
            }
            throw new Exception("Could not find access token in response: " + respStr);
        }
    }
}
