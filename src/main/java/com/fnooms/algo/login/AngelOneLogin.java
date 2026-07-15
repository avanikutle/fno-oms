package com.fnooms.algo.login;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;

import java.util.concurrent.TimeUnit;

public class AngelOneLogin implements BrokerLogin {

    private static final String LOGIN_URL = "https://apiconnect.angelbroking.com/rest/auth/angelbroking/user/v1/loginByPassword";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient http = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();

    @Override
    public String login(String userId, String password, String totp) throws Exception {
        throw new UnsupportedOperationException("Needs API Key for AngelOne login");
    }

    public String[] login(String userId, String password, String totp, String apiKey) throws Exception {
        JsonObject payload = new JsonObject();
        payload.addProperty("clientcode", userId);
        payload.addProperty("password", password); // usually MPIN
        payload.addProperty("totp", totp);

        RequestBody body = RequestBody.create(payload.toString(), JSON);

        Request request = new Request.Builder()
                .url(LOGIN_URL)
                .post(body)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("X-PrivateKey", apiKey)
                .header("X-MACAddress", "00-00-00-00-00-00")
                .header("X-ClientLocalIP", "127.0.0.1")
                .header("X-ClientPublicIP", "127.0.0.1")
                .header("X-UserType", "USER")
                .header("X-SourceID", "WEB")
                .build();

        try (Response response = http.newCall(request).execute()) {
            String respStr = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new Exception("AngelOne Login failed: " + response.code() + " - " + respStr);
            }

            JsonObject json = JsonParser.parseString(respStr).getAsJsonObject();
            if (json.has("status") && json.get("status").getAsBoolean()) {
                JsonElement dataEl = json.get("data");
                if (dataEl != null && dataEl.isJsonObject()) {
                    JsonObject data = dataEl.getAsJsonObject();
                    if (data.has("jwtToken") && data.has("feedToken")) {
                        return new String[]{ data.get("jwtToken").getAsString(), data.get("feedToken").getAsString() };
                    }
                }
            }
            throw new Exception("AngelOne Login unsuccessful or missing tokens: " + respStr);
        }
    }
}
