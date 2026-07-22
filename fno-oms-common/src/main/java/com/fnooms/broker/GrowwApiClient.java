package com.fnooms.broker;

import com.fnooms.model.BrokerContext;
import com.fnooms.model.StandardHolding;
import com.fnooms.model.dto.GrowwHoldingDTO;
import com.fnooms.model.dto.GrowwHoldingsResponseDTO;
import com.fnooms.util.BrokerConfig;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrowwApiClient implements IBrokerApiClient {
    private static final Logger log = LoggerFactory.getLogger(GrowwApiClient.class);
    private final OkHttpClient client;
    private final Gson gson;
    private BrokerContext context;

    public GrowwApiClient() {
        this.client = new OkHttpClient();
        this.gson = new Gson();
    }

    @Override
    public void init(BrokerContext context) {
        this.context = context;
    }

    @Override
    public boolean login() throws Exception {
        if (context == null || context.getApiKey() == null || context.getTotp() == null) {
            throw new IllegalStateException("Client must be initialized with an API Key and TOTP.");
        }

        String url = BrokerConfig.getProperty("groww.api.login");
        String jsonPayload = String.format("{\"key_type\": \"totp\", \"totp\": \"%s\"}", context.getTotp());
        
        okhttp3.RequestBody body = okhttp3.RequestBody.create(jsonPayload, okhttp3.MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Authorization", "Bearer " + context.getApiKey())
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                log.error("Failed to generate access token: {} - {}", response.code(), responseBody);
                return false;
            }
            
            com.google.gson.JsonObject jsonResponse = com.google.gson.JsonParser.parseString(responseBody).getAsJsonObject();
            if (jsonResponse.has("token")) {
                String accessToken = jsonResponse.get("token").getAsString();
                context.setAccessToken(accessToken);
                return true;
            }
            
            log.error("Access token not found in response: {}", responseBody);
            return false;
        }
    }

    @Override
    public List<StandardHolding> getHoldings() throws Exception {
        if (context == null || context.getAccessToken() == null) {
            throw new IllegalStateException("Client not initialized with a valid access token.");
        }

        String url = BrokerConfig.getProperty("groww.api.holdings");
        
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Bearer " + context.getAccessToken())
                .addHeader("X-API-VERSION", "1.0")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                log.error("Failed to fetch holdings: {} - {}", response.code(), responseBody);
                throw new Exception("Groww API request failed");
            }
            
            GrowwHoldingsResponseDTO dto = gson.fromJson(responseBody, GrowwHoldingsResponseDTO.class);
            List<StandardHolding> standardHoldings = new ArrayList<>();
            
            if (dto != null && "SUCCESS".equals(dto.getStatus()) && dto.getPayload() != null && dto.getPayload().getHoldings() != null) {
                for (GrowwHoldingDTO gDTO : dto.getPayload().getHoldings()) {
                    StandardHolding sh = new StandardHolding();
                    sh.setIsin(gDTO.getIsin());
                    sh.setTradingSymbol(gDTO.getTrading_symbol());
                    sh.setQuantity(gDTO.getQuantity());
                    sh.setAveragePrice(gDTO.getAverage_price());
                    standardHoldings.add(sh);
                }
            }
            return standardHoldings;
        }
    }
    @Override
    public String getUserDetails() throws Exception {
        if (context == null || context.getAccessToken() == null) {
            throw new IllegalStateException("Client not initialized with a valid access token.");
        }

        String url = BrokerConfig.getProperty("groww.api.profile");
        
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Bearer " + context.getAccessToken())
                .addHeader("X-API-VERSION", "1.0")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                log.error("Failed to fetch user details: {} - {}", response.code(), responseBody);
                throw new Exception("Groww API request failed");
            }
            return responseBody;
        }
    }
}
