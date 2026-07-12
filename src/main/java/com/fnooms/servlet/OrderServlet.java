package com.fnooms.servlet;

import com.fnooms.broker.BrokerException;
import com.fnooms.broker.dto.OrderRequest;
import com.fnooms.broker.dto.OrderResponse;
import com.fnooms.service.OrderService;
import com.fnooms.util.JsonUtil;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

/**
 * REST endpoint for order operations.
 *
 * GET    /api/orders          → fetch live order book from broker
 * POST   /api/orders          → place a new order
 * DELETE /api/orders/{id}     → cancel an order
 * GET    /api/orders/local    → fetch today's orders from local DB
 */
public class OrderServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(OrderServlet.class);
    private final OrderService orderService = new OrderService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        try {
            if ("/local".equals(path)) {
                JsonUtil.writeJson(resp, 200,
                        JsonUtil.success(orderService.getLocalOrderHistory()));
            } else {
                List<OrderResponse> orders = orderService.getOrderBook();
                JsonUtil.writeJson(resp, 200, JsonUtil.success(orders));
            }
        } catch (IllegalStateException e) {
            // No active broker configured
            JsonUtil.writeJson(resp, 503, JsonUtil.error(e.getMessage()));
        } catch (BrokerException e) {
            log.warn("GetOrderBook failed: {}", e.getMessage());
            JsonUtil.writeJson(resp, 502, JsonUtil.error(e.getMessage()));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            // Parse JSON body
            String body = new String(req.getInputStream().readAllBytes());
            JsonObject json = JsonUtil.parseObject(body);

            OrderRequest order = new OrderRequest();
            order.setSymbol(getString(json, "symbol"));
            order.setExchange(getString(json, "exchange"));
            order.setTransactionType(getString(json, "transactionType"));
            order.setOrderType(getString(json, "orderType"));
            order.setProduct(getString(json, "product"));
            order.setQuantity(getInt(json, "quantity"));
            order.setValidity(getString(json, "validity") != null ? getString(json, "validity") : "DAY");

            if (json.has("price") && !json.get("price").isJsonNull())
                order.setPrice(json.get("price").getAsBigDecimal());
            if (json.has("triggerPrice") && !json.get("triggerPrice").isJsonNull())
                order.setTriggerPrice(json.get("triggerPrice").getAsBigDecimal());
            if (json.has("tag") && !json.get("tag").isJsonNull())
                order.setTag(json.get("tag").getAsString());

            // Validate
            if (order.getSymbol() == null || order.getSymbol().isBlank())
                throw new IllegalArgumentException("symbol is required");
            if (order.getQuantity() <= 0)
                throw new IllegalArgumentException("quantity must be > 0");

            OrderResponse result = orderService.placeOrder(order);
            JsonUtil.writeJson(resp, 200, JsonUtil.success(result));

        } catch (IllegalArgumentException e) {
            JsonUtil.writeJson(resp, 400, JsonUtil.error(e.getMessage()));
        } catch (IllegalStateException e) {
            JsonUtil.writeJson(resp, 503, JsonUtil.error(e.getMessage()));
        } catch (BrokerException e) {
            log.warn("PlaceOrder failed: {}", e.getMessage());
            JsonUtil.writeJson(resp, 502, JsonUtil.error(e.getMessage()));
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo(); // e.g. "/ORD123456"
        if (path == null || path.equals("/")) {
            JsonUtil.writeJson(resp, 400, JsonUtil.error("Order ID required in path"));
            return;
        }
        String orderId = path.substring(1); // strip leading /
        try {
            boolean cancelled = orderService.cancelOrder(orderId);
            JsonUtil.writeJson(resp, 200, JsonUtil.success(cancelled));
        } catch (BrokerException e) {
            log.warn("CancelOrder {} failed: {}", orderId, e.getMessage());
            JsonUtil.writeJson(resp, 502, JsonUtil.error(e.getMessage()));
        }
    }

    private String getString(JsonObject o, String key) {
        return (o.has(key) && !o.get(key).isJsonNull()) ? o.get(key).getAsString() : null;
    }

    private int getInt(JsonObject o, String key) {
        return (o.has(key) && !o.get(key).isJsonNull()) ? o.get(key).getAsInt() : 0;
    }
}
