package com.fnooms.algo.login;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.concurrent.TimeUnit;
import okhttp3.*;

public class DhanLogin implements BrokerLogin {

    private static final String LOGIN_URL = "https://auth.dhan.co/app/generateAccessToken";

    private final OkHttpClient http = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();

    @Override
    public String login(String clientId, String pin, String totp) throws Exception {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(LOGIN_URL).newBuilder();
        urlBuilder.addQueryParameter("dhanClientId", clientId);
        urlBuilder.addQueryParameter("pin", pin); 
        urlBuilder.addQueryParameter("totp", totp);

        String url = urlBuilder.build().toString();
        RequestBody body = RequestBody.create("", MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("Accept", "application/json")
                .build();

        try (Response response = http.newCall(request).execute()) {
            String respStr = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new Exception("Dhan Login failed: " + response.code() + " - " + respStr);
            }

            JsonObject json = JsonParser.parseString(respStr).getAsJsonObject();
            
            // Dhan usually returns token in a field like accessToken or within a data block
            if (json.has("accessToken")) {
                return json.get("accessToken").getAsString();
            } else if (json.has("data")) {
                JsonElement dataEl = json.get("data");
                if (dataEl.isJsonObject()) {
                    JsonObject data = dataEl.getAsJsonObject();
                    if (data.has("accessToken")) {
                        return data.get("accessToken").getAsString();
                    } else if (data.has("access_token")) {
                        return data.get("access_token").getAsString();
                    }
                }
            }
            
            throw new Exception("Could not parse accessToken from response: " + respStr);
        }
    }
}
