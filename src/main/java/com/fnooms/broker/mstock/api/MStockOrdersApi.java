package com.fnooms.broker.mstock.api;

import com.fnooms.broker.BrokerException;
import com.fnooms.broker.dto.OrderRequest;
import com.fnooms.broker.dto.OrderResponse;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles mStock API endpoints for Orders.
 */
public class MStockOrdersApi {

    private static final Logger log = LoggerFactory.getLogger(MStockOrdersApi.class);
    private final MStockCoreClient core;

    public MStockOrdersApi(MStockCoreClient core) {
        this.core = core;
    }

    private String mapProductType(String product, String exchange) {
        if (product == null) return "CARRYFORWARD";
        String p = product.toUpperCase();
        if ("NRML".equals(p)) {
            return ("NSE".equalsIgnoreCase(exchange) || "BSE".equalsIgnoreCase(exchange)) ? "DELIVERY" : "CARRYFORWARD";
        } else if ("MIS".equals(p)) {
            return "INTRADAY";
        } else if ("CNC".equals(p)) {
            return "DELIVERY";
        }
        return p;
    }

    public OrderResponse placeOrder(OrderRequest req) throws BrokerException {
        JsonObject payload = new JsonObject();
        String token = com.fnooms.algo.ScripMasterService.getToken(req.getSymbol());
        
        payload.addProperty("variety", "NORMAL");
        payload.addProperty("tradingsymbol", req.getSymbol());
        payload.addProperty("symboltoken", token != null ? token : req.getSymbol());
        payload.addProperty("exchange", req.getExchange());
        payload.addProperty("transactiontype", req.getTransactionType());
        payload.addProperty("ordertype", req.getOrderType());
        payload.addProperty("producttype", mapProductType(req.getProduct(), req.getExchange()));
        payload.addProperty("quantity", String.valueOf(req.getQuantity()));
        payload.addProperty("duration", req.getValidity() != null ? req.getValidity() : "DAY");

        payload.addProperty("price", req.getPrice() != null ? String.valueOf(req.getPrice()) : "0");
        payload.addProperty("triggerprice", req.getTriggerPrice() != null ? String.valueOf(req.getTriggerPrice()) : "0");
        payload.addProperty("squareoff", "0");
        payload.addProperty("stoploss", "0");
        payload.addProperty("trailingStopLoss", "0");
        payload.addProperty("disclosedquantity", "0");
        payload.addProperty("ordertag", req.getTag() != null ? req.getTag() : "");

        log.info("MStock placeOrder payload: {}", payload.toString());
        JsonObject resp = core.executePost(core.getBaseUrl() + "/orders/regular", payload.toString());
        log.info("MStock placeOrder response: {}", resp.toString());

        OrderResponse order = new OrderResponse();
        if (resp.has("data")) {
            if (resp.get("data").isJsonObject()) {
                JsonObject data = resp.getAsJsonObject("data");
                if (data.has("order_id")) {
                    order.setBrokerOrderId(data.get("order_id").getAsString());
                } else if (data.has("orderId")) {
                    order.setBrokerOrderId(data.get("orderId").getAsString());
                } else {
                    order.setBrokerOrderId("UNKNOWN_" + System.currentTimeMillis());
                }
            } else if (resp.get("data").isJsonPrimitive()) {
                order.setBrokerOrderId(resp.get("data").getAsString());
            }
        } else {
            // If it succeeded but we can't find the ID, prevent retry loop by setting a dummy ID
            order.setBrokerOrderId("UNKNOWN_" + System.currentTimeMillis());
        }

        order.setSymbol(req.getSymbol());
        order.setExchange(req.getExchange());
        order.setTransactionType(req.getTransactionType());
        order.setOrderType(req.getOrderType());
        order.setProduct(req.getProduct());
        order.setQuantity(req.getQuantity());
        order.setPrice(req.getPrice());
        order.setTriggerPrice(req.getTriggerPrice());
        order.setStatus("OPEN");
        order.setOrderTimestamp(Instant.now());

        log.info("Order placed: {} {} {} {}@{}", req.getTransactionType(), req.getQuantity(),
                req.getSymbol(), req.getOrderType(), req.getPrice());
        return order;
    }

    public List<OrderResponse> getOrderBook() throws BrokerException {
        JsonObject body = core.executeGet(core.getBaseUrl() + "/orders");
        List<OrderResponse> orders = new ArrayList<>();

        JsonArray data = core.safeGetArray(body, "data");
        if (data == null) return orders;

        for (JsonElement el : data) {
            JsonObject o = el.getAsJsonObject();
            OrderResponse resp = new OrderResponse();
            resp.setBrokerOrderId(core.safeString(o, "order_id"));
            resp.setExchangeOrderId(core.safeString(o, "exchange_order_id"));
            resp.setSymbol(core.safeString(o, "tradingsymbol"));
            resp.setExchange(core.safeString(o, "exchange"));
            resp.setTransactionType(core.safeString(o, "transaction_type"));
            resp.setOrderType(core.safeString(o, "order_type"));
            resp.setProduct(core.safeString(o, "product"));
            resp.setQuantity(core.safeInt(o, "quantity"));
            resp.setFilledQuantity(core.safeInt(o, "filled_quantity"));
            resp.setPendingQuantity(core.safeInt(o, "pending_quantity"));
            resp.setPrice(core.safeDecimal(o, "price"));
            resp.setTriggerPrice(core.safeDecimal(o, "trigger_price"));
            resp.setAveragePrice(core.safeDecimal(o, "average_price"));
            resp.setStatus(core.safeString(o, "status"));
            resp.setStatusMessage(core.safeString(o, "status_message"));
            resp.setValidity(core.safeString(o, "validity"));
            resp.setTag(core.safeString(o, "tag"));
            orders.add(resp);
        }

        return orders;
    }

    public boolean cancelOrder(String brokerOrderId) throws BrokerException {
        core.executeDelete(core.getBaseUrl() + "/orders/regular/" + brokerOrderId, null);
        return true;
    }

    public OrderResponse modifyOrder(String brokerOrderId, OrderRequest updated) throws BrokerException {
        JsonObject payload = new JsonObject();
        payload.addProperty("variety", "NORMAL");
        if (updated.getQuantity() > 0)      payload.addProperty("quantity",      String.valueOf(updated.getQuantity()));
        if (updated.getPrice() != null)      payload.addProperty("price",         String.valueOf(updated.getPrice()));
        if (updated.getTriggerPrice() != null) payload.addProperty("triggerprice", String.valueOf(updated.getTriggerPrice()));
        if (updated.getOrderType() != null)  payload.addProperty("ordertype",    updated.getOrderType());
        payload.addProperty("duration", updated.getValidity() != null ? updated.getValidity() : "DAY");

        core.executePut(core.getBaseUrl() + "/orders/regular/" + brokerOrderId, payload.toString());

        OrderResponse order = new OrderResponse();
        order.setBrokerOrderId(brokerOrderId);
        order.setStatus("MODIFIED");
        return order;
    }

    // STUB: Trade Book
    // public List<TradeResponse> getTradeBook() throws BrokerException { ... }
}
