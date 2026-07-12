package com.fnooms.service;

import com.fnooms.async.OrderEventBus;
import com.fnooms.async.AuditEventBus;
import com.fnooms.async.event.AuditEvent;
import com.fnooms.async.event.OrderEvent;
import com.fnooms.broker.BrokerClient;
import com.fnooms.broker.BrokerClientFactory;
import com.fnooms.broker.BrokerException;
import com.fnooms.broker.dto.OrderRequest;
import com.fnooms.broker.dto.OrderResponse;
import com.fnooms.dao.BrokerConfigDAO;
import com.fnooms.dao.OrderDAO;
import com.fnooms.model.BrokerConfig;
import com.fnooms.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Orchestrates order placement and retrieval.
 *
 * <p>Flow for placeOrder:
 * <ol>
 *   <li>Get active BrokerClient (sync, fast)</li>
 *   <li>Call broker API (sync, ~200ms network)</li>
 *   <li>Publish OrderEvent to async bus → returns immediately</li>
 *   <li>Publish AuditEvent to async bus → returns immediately</li>
 *   <li>Return OrderResponse to caller</li>
 * </ol>
 * DB writes never block the order flow.
 */
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private final BrokerConfigDAO configDAO = new BrokerConfigDAO();
    private final OrderDAO        orderDAO  = new OrderDAO();

    public OrderResponse placeOrder(OrderRequest request) throws BrokerException {
        BrokerConfig activeCfg = configDAO.getActive();
        if (activeCfg == null) {
            throw new BrokerException("No active broker configured. Please go to Settings.");
        }

        BrokerClient client = BrokerClientFactory.getClientFor(activeCfg);
        long start = System.currentTimeMillis();

        // ── SYNCHRONOUS: actual order placement ──────────────────────────
        OrderResponse response = client.placeOrder(request);
        long latency = System.currentTimeMillis() - start;
        // ─────────────────────────────────────────────────────────────────

        // ── ASYNC: persist to DB (fire-and-forget, never blocks) ─────────
        OrderEventBus.getInstance().publish(
                new OrderEvent(OrderEvent.Type.PLACED, request, response, activeCfg.getBrokerType(), activeCfg.getId()));

        AuditEventBus.getInstance().publish(
                new AuditEvent("PLACE_ORDER", activeCfg.getBrokerType(),
                        "/orders", request.toString(), response.toString(),
                        200, latency, null));
        // ─────────────────────────────────────────────────────────────────

        log.info("Order placed in {}ms: {} {} {} @ {} | brokerOrderId={}",
                latency, request.getTransactionType(), request.getQuantity(),
                request.getSymbol(), request.getPrice(), response.getBrokerOrderId());

        return response;
    }

    public List<OrderResponse> getOrderBook() throws BrokerException {
        BrokerClient client = BrokerClientFactory.getActiveClient();
        List<OrderResponse> orders = client.getOrderBook();

        AuditEventBus.getInstance().publish(
                new AuditEvent("GET_ORDER_BOOK", client.getBrokerType().name(),
                        "/orders", null, "count=" + orders.size(), 200, 0, null));

        return orders;
    }

    public boolean cancelOrder(String brokerOrderId) throws BrokerException {
        BrokerConfig activeCfg = configDAO.getActive();
        BrokerClient client = BrokerClientFactory.getClientFor(activeCfg);
        boolean result = client.cancelOrder(brokerOrderId);

        AuditEventBus.getInstance().publish(
                new AuditEvent("CANCEL_ORDER", activeCfg.getBrokerType(),
                        "/orders/" + brokerOrderId, null,
                        String.valueOf(result), 200, 0, null));

        return result;
    }

    /** Fetch today's orders from local DB (fast, no broker API call). */
    public List<Order> getLocalOrderHistory() {
        return orderDAO.findTodaysOrders();
    }
}
