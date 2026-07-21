package com.fnooms.amock;

import com.fnooms.broker.BrokerClient;
import com.fnooms.broker.BrokerException;
import com.fnooms.broker.BrokerType;
import com.fnooms.broker.dto.*;
import com.google.gson.Gson;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MockBrokerClient implements BrokerClient {
    private static final String BASE_URL = "http://localhost:9090/mock";
    private final HttpClient httpClient;
    private final Gson gson;

    public MockBrokerClient(String prefix) {
        this.httpClient = HttpClient.newBuilder().build();
        this.gson = new Gson();
    }

    @Override
    public BrokerType getBrokerType() {
        return BrokerType.MOCK;
    }

    @Override
    public Map<String, Quote> getQuotes(List<String> instruments) throws BrokerException {
        // Mock server doesn't provide quotes right now. In a real app, this would hit the mock tick server.
        // We will return dummy quotes.
        Map<String, Quote> map = new HashMap<>();
        for (String inst : instruments) {
            Quote q = new Quote();
            q.setSymbol(inst.split(":")[1]);
            q.setExchange(inst.split(":")[0]);
            q.setLtp(BigDecimal.valueOf(15000.0));
            map.put(inst, q);
        }
        return map;
    }

    @Override
    public OrderResponse placeOrder(OrderRequest request) throws BrokerException {
        try {
            String json = gson.toJson(request);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/orders"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                Map<String, Object> map = gson.fromJson(resp.body(), Map.class);
                if ("success".equals(map.get("status"))) {
                    Map<String, Object> data = (Map<String, Object>) map.get("data");
                    OrderResponse r = new OrderResponse();
                    r.setBrokerOrderId((String) data.get("brokerOrderId"));
                    r.setStatus("OPEN");
                    return r;
                }
            }
            throw new BrokerException("Mock Server rejected order: " + resp.body());
        } catch (Exception e) {
            throw new BrokerException("Failed to place mock order", e);
        }
    }

    @Override
    public List<OrderResponse> getOrderBook() throws BrokerException {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/orders"))
                    .GET()
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                Map<String, Object> map = gson.fromJson(resp.body(), Map.class);
                if ("success".equals(map.get("status"))) {
                    List<Map<String, Object>> dataList = (List<Map<String, Object>>) map.get("data");
                    return dataList.stream().map(data -> {
                        OrderResponse r = new OrderResponse();
                        r.setBrokerOrderId((String) data.get("brokerOrderId"));
                        r.setStatus((String) data.get("status"));
                        r.setSymbol((String) data.get("symbol"));
                        r.setTransactionType((String) data.get("transactionType"));
                        r.setOrderType((String) data.get("orderType"));
                        r.setProduct((String) data.get("product"));
                        
                        Object price = data.get("price");
                        if (price != null) r.setPrice(BigDecimal.valueOf(((Number) price).doubleValue()));
                        
                        Object quantity = data.get("quantity");
                        if (quantity != null) r.setQuantity(((Number) quantity).intValue());
                        
                        Object filledQuantity = data.get("filledQuantity");
                        if (filledQuantity != null) r.setFilledQuantity(((Number) filledQuantity).intValue());
                        
                        Object averagePrice = data.get("averagePrice");
                        if (averagePrice != null) r.setAveragePrice(BigDecimal.valueOf(((Number) averagePrice).doubleValue()));

                        return r;
                    }).collect(Collectors.toList());
                }
            }
            throw new BrokerException("Failed to fetch mock order book: " + resp.body());
        } catch (Exception e) {
            throw new BrokerException("Failed to fetch mock order book", e);
        }
    }

    @Override
    public boolean cancelOrder(String brokerOrderId) throws BrokerException {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/orders/" + brokerOrderId))
                    .DELETE()
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                Map<String, Object> map = gson.fromJson(resp.body(), Map.class);
                return "success".equals(map.get("status"));
            }
            return false;
        } catch (Exception e) {
            throw new BrokerException("Failed to cancel mock order", e);
        }
    }

    @Override
    public OrderResponse modifyOrder(String brokerOrderId, OrderRequest updated) throws BrokerException {
        // Not implemented in Mock server yet
        throw new BrokerException("modifyOrder not implemented in MOCK broker");
    }

    @Override
    public List<Position> getPositions() throws BrokerException {
        return new ArrayList<>(); // return empty for now
    }

    @Override
    public List<Holding> getHoldings() throws BrokerException {
        return new ArrayList<>(); // return empty for now
    }

    @Override
    public void testConnection() throws BrokerException {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/user/profile"))
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                throw new BrokerException("Mock server returned status: " + resp.statusCode());
            }
        } catch (Exception e) {
            throw new BrokerException("Failed to connect to Mock Server", e);
        }
    }
}
