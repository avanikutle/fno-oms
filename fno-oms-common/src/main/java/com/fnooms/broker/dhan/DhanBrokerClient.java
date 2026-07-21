package com.fnooms.broker.dhan;

import com.fnooms.algo.ScripMasterServiceProvider;
import com.fnooms.broker.BrokerClient;
import com.fnooms.broker.BrokerException;
import com.fnooms.broker.BrokerType;
import com.fnooms.broker.dto.Holding;
import com.fnooms.broker.dto.OrderRequest;
import com.fnooms.broker.dto.OrderResponse;
import com.fnooms.broker.dto.Quote;
import com.fnooms.dao.AlgoKeyValueDAO;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DhanBrokerClient implements BrokerClient {
    private static final Logger log = LoggerFactory.getLogger(DhanBrokerClient.class);
    private static final String API_BASE_URL = "https://api.dhan.co/v2";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private final OkHttpClient http;
    private final AlgoKeyValueDAO dao;
    private final String clientId;
    private final String accessToken;
    private final String prefix;

    public DhanBrokerClient(String prefix) {
        this.prefix = prefix;
        this.dao = new AlgoKeyValueDAO();
        this.clientId = dao.getValue("dhan.client_id");
        this.accessToken = dao.getValue("dhan.access_token");

        if (clientId == null || accessToken == null) {
            throw new IllegalStateException("Dhan client ID or access token not found in configuration.");
        }

        this.http = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public BrokerType getBrokerType() {
        return BrokerType.DHAN;
    }

    @Override
    public void testConnection() throws BrokerException {
        try {
            getOrderBook();
        } catch (Exception e) {
            log.error("Dhan connection test failed", e);
            throw new BrokerException("Connection test failed", e);
        }
    }

    @Override
    public List<Holding> getHoldings() throws BrokerException {
        return new ArrayList<>(); // Stub for now
    }

    @Override
    public List<com.fnooms.broker.dto.Position> getPositions() throws BrokerException {
        return new ArrayList<>(); // Stub for now
    }

    @Override
    public OrderResponse placeOrder(OrderRequest request) throws BrokerException {
        JsonObject payload = new JsonObject();
        payload.addProperty("dhanClientId", clientId);
        payload.addProperty("correlationId", "oms_" + System.currentTimeMillis());
        payload.addProperty("transactionType", request.getTransactionType());
        // Default to NSE_FNO for FnO OMS
        payload.addProperty("exchangeSegment", "NSE_FNO");
        payload.addProperty("productType", "INTRADAY");
        payload.addProperty("orderType", request.getPrice() == null || request.getPrice().compareTo(java.math.BigDecimal.ZERO) <= 0 ? "MARKET" : "LIMIT");
        payload.addProperty("validity", "DAY");
        
        String token = ScripMasterServiceProvider.getInstance("DHAN").getToken(request.getSymbol());
        if (token == null) {
            throw new BrokerException("Could not find Dhan token for symbol: " + request.getSymbol());
        }
        payload.addProperty("securityId", token);
        
        payload.addProperty("quantity", request.getQuantity());
        payload.addProperty("disclosedQuantity", 0);
        payload.addProperty("price", request.getPrice() == null ? 0.0 : request.getPrice().doubleValue());
        payload.addProperty("triggerPrice", 0.0);
        payload.addProperty("afterMarketOrder", false);

        RequestBody body = RequestBody.create(payload.toString(), JSON);
        Request httpRequest = new Request.Builder()
                .url(API_BASE_URL + "/orders")
                .post(body)
                .header("Content-Type", "application/json")
                .header("access-token", accessToken)
                .build();

        try (Response response = http.newCall(httpRequest).execute()) {
            String respStr = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new BrokerException("Dhan order placement failed: " + response.code() + " - " + respStr);
            }
            
            JsonObject json = JsonParser.parseString(respStr).getAsJsonObject();
            OrderResponse orderResponse = new OrderResponse();
            if (json.has("orderId")) {
                orderResponse.setBrokerOrderId(json.get("orderId").getAsString());
                orderResponse.setStatus("OPEN");
                orderResponse.setStatusMessage("Order Placed Successfully");
            } else if (json.has("data") && json.getAsJsonObject("data").has("orderId")) {
                orderResponse.setBrokerOrderId(json.getAsJsonObject("data").get("orderId").getAsString());
                orderResponse.setStatus("OPEN");
                orderResponse.setStatusMessage("Order Placed Successfully");
            } else {
                orderResponse.setStatus("REJECTED");
                orderResponse.setStatusMessage("Response format invalid: " + respStr);
            }
            return orderResponse;
        } catch (IOException e) {
            throw new BrokerException("Network error while placing Dhan order", e);
        }
    }

    @Override
    public List<OrderResponse> getOrderBook() throws BrokerException {
        Request httpRequest = new Request.Builder()
                .url(API_BASE_URL + "/orders")
                .get()
                .header("access-token", accessToken)
                .build();

        try (Response response = http.newCall(httpRequest).execute()) {
            String respStr = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new BrokerException("Dhan getOrderBook failed: " + response.code() + " - " + respStr);
            }
            
            List<OrderResponse> orders = new ArrayList<>();
            // Assuming Dhan returns an array or an object with a data array
            JsonElement el = JsonParser.parseString(respStr);
            JsonArray arr = null;
            if (el.isJsonArray()) {
                arr = el.getAsJsonArray();
            } else if (el.isJsonObject() && el.getAsJsonObject().has("data")) {
                arr = el.getAsJsonObject().getAsJsonArray("data");
            }
            
            if (arr != null) {
                for (JsonElement item : arr) {
                    JsonObject obj = item.getAsJsonObject();
                    OrderResponse or = new OrderResponse();
                    if (obj.has("orderId")) or.setBrokerOrderId(obj.get("orderId").getAsString());
                    if (obj.has("orderStatus")) or.setStatus(obj.get("orderStatus").getAsString());
                    orders.add(or);
                }
            }
            return orders;
        } catch (IOException e) {
            throw new BrokerException("Network error while fetching Dhan order book", e);
        }
    }

    @Override
    public boolean cancelOrder(String brokerOrderId) throws BrokerException {
        Request httpRequest = new Request.Builder()
                .url(API_BASE_URL + "/orders/" + brokerOrderId)
                .delete()
                .header("access-token", accessToken)
                .build();

        try (Response response = http.newCall(httpRequest).execute()) {
            return response.isSuccessful();
        } catch (IOException e) {
            throw new BrokerException("Network error while cancelling Dhan order", e);
        }
    }

    @Override
    public OrderResponse modifyOrder(String brokerOrderId, OrderRequest request) throws BrokerException {
        throw new BrokerException("Modify order not yet implemented for Dhan");
    }

    @Override
    public java.util.Map<String, Quote> getQuotes(List<String> symbols) throws BrokerException {
        return new java.util.HashMap<>(); // Stub for now
    }
}
