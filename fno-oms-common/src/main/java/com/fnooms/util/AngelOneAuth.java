package com.fnooms.util;

import com.google.gson.JsonObject;
import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AngelOneAuth {

    public static String loginAndGetJwt(String clientPin, String totpCode) throws IOException {
        OkHttpClient client = new OkHttpClient().newBuilder().build();
        MediaType mediaType = MediaType.parse("application/json");

        String prefix = "angelone";
        String clientId = CredsUtil.getAngelOneClientCode(prefix);
        String apiKey = CredsUtil.getApiKey(prefix);

        // Construct the JSON body
        JsonObject requestBodyJson = new JsonObject();
        requestBodyJson.addProperty("clientcode", clientId);
        requestBodyJson.addProperty("password", clientPin);
        requestBodyJson.addProperty("totp", totpCode);
        requestBodyJson.addProperty("state", "STATE_VARIABLE");

        // OkHttp 4.x style for creating RequestBody (String, MediaType)
        // For OkHttp 3.x it was (MediaType, String). The below uses the older compat
        // signature which user provided.
        @SuppressWarnings("deprecation")
        RequestBody body = RequestBody.create(mediaType, requestBodyJson.toString());

        Request request = new Request.Builder()
                .url("https://apiconnect.angelone.in/rest/auth/angelbroking/user/v1/loginByPassword")
                .method("POST", body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .addHeader("X-UserType", "USER")
                .addHeader("X-SourceID", "WEB")
                .addHeader("X-ClientLocalIP", "127.0.0.1")
                .addHeader("X-ClientPublicIP", "127.0.0.1")
                .addHeader("X-MACAddress", "00:00:00:00:00:00")
                .addHeader("X-PrivateKey", apiKey)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                System.err.println("Login Failed: Unexpected HTTP code " + response.code() + " - " + errorBody);
                throw new IOException("Unexpected code " + response);
            }

            String responseBody = response.body().string();
            System.out.println("Login Response: " + responseBody);

            JsonObject jsonResponse = JsonUtil.parseObject(responseBody);

            if (jsonResponse.has("status") && jsonResponse.get("status").getAsBoolean()) {
                JsonObject data = jsonResponse.getAsJsonObject("data");
                String jwtToken = data.get("jwtToken").getAsString();
                String feedToken = data.get("feedToken").getAsString();
                System.out.println();
                System.out.println();
                System.out.println();

                System.out.println("angelone.jwt_token=" + jwtToken);
                System.out.println("angelone.feed_token=" + feedToken);

                return jwtToken;
            } else {
                System.err.println("Login failed API side: " + responseBody);
                return null;
            }
        }
    }

    public static void main(String[] args) {
        try {
            // Test the method (requires actual pin and totp);
            String jwt = loginAndGetJwt("3891", "163110");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
