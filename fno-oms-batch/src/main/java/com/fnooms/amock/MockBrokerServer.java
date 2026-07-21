package com.fnooms.amock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockBrokerServer {
    private static final Logger log = LoggerFactory.getLogger(MockBrokerServer.class);
    private static final int PORT = 9090;
    private static final String STATE_FILE = "mock_broker_state.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // In-memory state
    private static final Map<String, Map<String, Object>> orders = new ConcurrentHashMap<>();

    public static void start() {
        try {
            loadState();

            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.createContext("/mock/orders", new OrdersHandler());
            server.createContext("/mock/portfolio/positions", new PositionsHandler());
            server.createContext("/mock/user/profile", new ProfileHandler());

            server.setExecutor(Executors.newFixedThreadPool(4));
            server.start();
            log.info("MockBrokerServer started on port {}", PORT);
        } catch (IOException e) {
            log.error("Failed to start MockBrokerServer", e);
        }
    }

    private static synchronized void loadState() {
        File file = new File(STATE_FILE);
        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                Type type = new TypeToken<Map<String, Map<String, Object>>>() {}.getType();
                Map<String, Map<String, Object>> loaded = gson.fromJson(reader, type);
                if (loaded != null) {
                    orders.putAll(loaded);
                    log.info("Loaded {} orders into MockBrokerServer state", orders.size());
                }
            } catch (Exception e) {
                log.error("Failed to load mock broker state", e);
            }
        }
    }

    private static synchronized void saveState() {
        try (Writer writer = new FileWriter(STATE_FILE)) {
            gson.toJson(orders, writer);
        } catch (IOException e) {
            log.error("Failed to save mock broker state", e);
        }
    }

    static class OrdersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if ("POST".equalsIgnoreCase(method)) {
                // Place Order
                InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                Map<String, Object> reqBody = gson.fromJson(isr, Map.class);
                
                String orderId = "MOCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                
                Map<String, Object> orderState = new HashMap<>(reqBody);
                orderState.put("brokerOrderId", orderId);
                orderState.put("status", "OPEN");
                orderState.put("filledQuantity", 0);
                orderState.put("averagePrice", 0.0);
                orderState.put("exchangeTimestamp", new Date().toString());
                
                orders.put(orderId, orderState);
                saveState();

                Map<String, Object> resp = new HashMap<>();
                resp.put("status", "success");
                Map<String, Object> data = new HashMap<>();
                data.put("brokerOrderId", orderId);
                resp.put("data", data);

                sendResponse(exchange, 200, resp);
            } else if ("GET".equalsIgnoreCase(method)) {
                // Get Order Book
                Map<String, Object> resp = new HashMap<>();
                resp.put("status", "success");
                resp.put("data", new ArrayList<>(orders.values()));
                sendResponse(exchange, 200, resp);
            } else if ("DELETE".equalsIgnoreCase(method)) {
                // Cancel Order
                String[] parts = path.split("/");
                String orderId = parts[parts.length - 1];
                Map<String, Object> order = orders.get(orderId);
                
                Map<String, Object> resp = new HashMap<>();
                if (order != null && "OPEN".equals(order.get("status"))) {
                    order.put("status", "CANCELLED");
                    saveState();
                    resp.put("status", "success");
                    sendResponse(exchange, 200, resp);
                } else {
                    resp.put("status", "error");
                    resp.put("message", "Order not found or not open");
                    sendResponse(exchange, 400, resp);
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    static class PositionsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "success");
            resp.put("data", new ArrayList<>()); // Mock empty positions
            sendResponse(exchange, 200, resp);
        }
    }

    static class ProfileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "success");
            Map<String, Object> data = new HashMap<>();
            data.put("clientId", "MOCK-USER");
            data.put("name", "Mock User");
            resp.put("data", data);
            sendResponse(exchange, 200, resp);
        }
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, Object responseObj) throws IOException {
        String json = gson.toJson(responseObj);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
