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

    public OrderResponse placeOrder(OrderRequest req) throws BrokerException {
        Map<String, String> formParams = new LinkedHashMap<>();
        formParams.put("tradingsymbol",    req.getSymbol());
        formParams.put("exchange",         req.getExchange());
        formParams.put("transaction_type", req.getTransactionType());
        formParams.put("order_type",       req.getOrderType());
        formParams.put("product",          req.getProduct());
        formParams.put("quantity",         String.valueOf(req.getQuantity()));
        formParams.put("validity",         req.getValidity() != null ? req.getValidity() : "DAY");

        if (req.getPrice() != null)
            formParams.put("price", String.valueOf(req.getPrice()));
        if (req.getTriggerPrice() != null)
            formParams.put("trigger_price", String.valueOf(req.getTriggerPrice()));
        if (req.getTag() != null)
            formParams.put("tag", req.getTag());

        JsonObject resp = core.executePostForm(core.getBaseUrl() + "/orders/regular", formParams);

        OrderResponse order = new OrderResponse();
        JsonObject data = core.safeGetObject(resp, "data");
        if (data != null && data.has("order_id")) {
            order.setBrokerOrderId(data.get("order_id").getAsString());
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
        JsonObject json = core.executeDelete(core.getBaseUrl() + "/orders/regular/" + brokerOrderId, null);
        return core.safeBoolean(json, "status");
    }

    public OrderResponse modifyOrder(String brokerOrderId, OrderRequest updated) throws BrokerException {
        JsonObject payload = new JsonObject();
        if (updated.getQuantity() > 0)      payload.addProperty("quantity",      updated.getQuantity());
        if (updated.getPrice() != null)      payload.addProperty("price",         updated.getPrice());
        if (updated.getTriggerPrice() != null) payload.addProperty("trigger_price", updated.getTriggerPrice());
        if (updated.getOrderType() != null)  payload.addProperty("order_type",    updated.getOrderType());

        core.executePut(core.getBaseUrl() + "/orders/regular/" + brokerOrderId, payload.toString());

        OrderResponse order = new OrderResponse();
        order.setBrokerOrderId(brokerOrderId);
        order.setStatus("MODIFIED");
        return order;
    }

    // STUB: Trade Book
    // public List<TradeResponse> getTradeBook() throws BrokerException { ... }
}
