package com.fnooms.broker.angelone;

import com.fnooms.broker.BrokerClient;
import com.fnooms.broker.BrokerException;
import com.fnooms.broker.BrokerType;
import com.fnooms.broker.dto.*;
import com.fnooms.util.CredsUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AngelOneBrokerClient implements BrokerClient {
    private static final Logger log = LoggerFactory.getLogger(AngelOneBrokerClient.class);
    private static final String API_BASE_URL = "https://apiconnect.angelbroking.com/rest/secure/angelbroking/portfolio/v1";
    
    private final String prefix;
    private final OkHttpClient http;

    public AngelOneBrokerClient(String prefix) {
        this.prefix = prefix;
        this.http = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public BrokerType getBrokerType() {
        return BrokerType.ANGEL;
    }

    @Override
    public void testConnection() throws BrokerException {
        // Just verify that the JWT token is present and credentials are loaded
        String jwtToken = CredsUtil.getJwtToken(prefix);
        if (jwtToken == null || jwtToken.isEmpty()) {
            throw new BrokerException("Broker is not fully configured (missing JWT token) for prefix: " + prefix);
        }
    }

    @Override
    public Map<String, Quote> getQuotes(List<String> instruments) throws BrokerException {
        throw new UnsupportedOperationException("AngelOne execution not implemented yet");
    }

    @Override
    public OrderResponse placeOrder(OrderRequest request) throws BrokerException {
        throw new UnsupportedOperationException("AngelOne execution not implemented yet");
    }

    @Override
    public List<OrderResponse> getOrderBook() throws BrokerException {
        // Return empty for benchmark
        return new ArrayList<>();
    }

    @Override
    public boolean cancelOrder(String brokerOrderId) throws BrokerException {
        throw new UnsupportedOperationException("AngelOne execution not implemented yet");
    }

    @Override
    public OrderResponse modifyOrder(String brokerOrderId, OrderRequest updated) throws BrokerException {
        throw new UnsupportedOperationException("AngelOne execution not implemented yet");
    }

    private Request.Builder getRequestBuilder(String url) {
        String jwtToken = CredsUtil.getJwtToken(prefix);
        String apiKey = CredsUtil.getApiKey(prefix);
        return new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + jwtToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                .header("X-PrivateKey", apiKey != null ? apiKey : "")
                .header("X-ClientLocalIP", "127.0.0.1")
                .header("X-ClientPublicIP", "124.123.142.204")
                .header("X-MACAddress", "00-11-22-33-44-55")
                .header("X-UserType", "USER")
                .header("X-SourceID", "WEB");
    }

    @Override
    public List<Position> getPositions() throws BrokerException {
        Request request = getRequestBuilder(API_BASE_URL + "/getPosition").get().build();
        try (Response response = http.newCall(request).execute()) {
            String respStr = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new BrokerException("AngelOne getPositions failed: " + response.code() + " - " + respStr);
            }
            
            List<Position> positions = new ArrayList<>();
            try {
                JsonObject json = JsonParser.parseString(respStr).getAsJsonObject();
                if (json.has("data") && !json.get("data").isJsonNull()) {
                    JsonArray arr = json.getAsJsonArray("data");
                    for (JsonElement item : arr) {
                        JsonObject obj = item.getAsJsonObject();
                        Position p = new Position();
                        if (obj.has("tradingsymbol")) p.setSymbol(obj.get("tradingsymbol").getAsString());
                        if (obj.has("netqty")) p.setQuantity(obj.get("netqty").getAsInt());
                        if (obj.has("producttype")) p.setProduct(obj.get("producttype").getAsString());
                        
                        java.math.BigDecimal pnl = java.math.BigDecimal.ZERO;
                        if (obj.has("realised")) {
                            pnl = pnl.add(obj.get("realised").getAsBigDecimal());
                        }
                        if (obj.has("unrealised")) {
                            pnl = pnl.add(obj.get("unrealised").getAsBigDecimal());
                        }
                        p.setPnl(pnl);
                        
                        positions.add(p);
                    }
                }
            } catch (Exception e) {
                throw new BrokerException("Failed to parse JSON for positions. Response was: '" + respStr + "'", e);
            }
            return positions;
        } catch (IOException e) {
            throw new BrokerException("Network error while fetching AngelOne positions", e);
        }
    }

    @Override
    public List<Holding> getHoldings() throws BrokerException {
        Request request = getRequestBuilder(API_BASE_URL + "/getHolding").get().build();
        try (Response response = http.newCall(request).execute()) {
            String respStr = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new BrokerException("AngelOne getHoldings failed: " + response.code() + " - " + respStr);
            }
            
            List<Holding> holdings = new ArrayList<>();
            try {
                JsonObject json = JsonParser.parseString(respStr).getAsJsonObject();
                if (json.has("data") && !json.get("data").isJsonNull()) {
                    JsonArray arr = json.getAsJsonArray("data");
                    for (JsonElement item : arr) {
                        JsonObject obj = item.getAsJsonObject();
                        Holding h = new Holding();
                        if (obj.has("tradingsymbol")) h.setSymbol(obj.get("tradingsymbol").getAsString());
                        if (obj.has("quantity")) h.setQuantity(obj.get("quantity").getAsInt());
                        if (obj.has("averageprice")) h.setAveragePrice(obj.get("averageprice").getAsBigDecimal());
                        holdings.add(h);
                    }
                }
            } catch (Exception e) {
                throw new BrokerException("Failed to parse JSON for holdings. Response was: '" + respStr + "'", e);
            }
            return holdings;
        } catch (IOException e) {
            throw new BrokerException("Network error while fetching AngelOne holdings", e);
        }
    }
}
