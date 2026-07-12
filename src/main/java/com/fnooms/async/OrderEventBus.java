package com.fnooms.async;

import com.fnooms.async.event.OrderEvent;
import com.fnooms.dao.OrderDAO;
import com.fnooms.model.Order;
import com.fnooms.util.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/**
 * Singleton bus for persisting order events asynchronously.
 * Order placement API calls publish here and return immediately.
 * The writer thread persists to the orders table in PostgreSQL.
 */
public class OrderEventBus extends AbstractEventBus<OrderEvent> {

    private static final Logger log = LoggerFactory.getLogger(OrderEventBus.class);
    private static volatile OrderEventBus instance;
    private final OrderDAO orderDAO = new OrderDAO();

    private OrderEventBus() {
        super(AppConfig.getInstance().getOrderQueueCapacity());
    }

    public static OrderEventBus getInstance() {
        if (instance == null) {
            synchronized (OrderEventBus.class) {
                if (instance == null) instance = new OrderEventBus();
            }
        }
        return instance;
    }

    @Override
    protected void processEvent(OrderEvent event) {
        try {
            Order order = mapToOrder(event);
            switch (event.getType()) {
                case PLACED   -> orderDAO.insert(order);
                case UPDATED  -> orderDAO.updateStatus(order);
                case CANCELLED -> orderDAO.updateStatus(order);
            }
        } catch (Exception e) {
            // CRITICAL: never lose order data — log fully for manual recovery
            log.error("FAILED to persist order event type={} brokerOrderId={} symbol={}. " +
                      "MANUAL RECOVERY NEEDED. Error: {}",
                    event.getType(),
                    event.getResponse() != null ? event.getResponse().getBrokerOrderId() : "null",
                    event.getRequest() != null ? event.getRequest().getSymbol() : "null",
                    e.getMessage(), e);
        }
    }

    private Order mapToOrder(OrderEvent event) {
        Order o = new Order();
        if (event.getResponse() != null) {
            o.setBrokerOrderId(event.getResponse().getBrokerOrderId());
            o.setStatus(event.getResponse().getStatus());
            o.setStatusMessage(event.getResponse().getStatusMessage());
            o.setFilledQuantity(event.getResponse().getFilledQuantity());
            o.setAveragePrice(event.getResponse().getAveragePrice());
            o.setExchangeOrderId(event.getResponse().getExchangeOrderId());
            o.setExchangeTimestamp(event.getResponse().getExchangeTimestamp());
        }
        if (event.getRequest() != null) {
            o.setSymbol(event.getRequest().getSymbol());
            o.setExchange(event.getRequest().getExchange());
            o.setTransactionType(event.getRequest().getTransactionType());
            o.setOrderType(event.getRequest().getOrderType());
            o.setProduct(event.getRequest().getProduct());
            o.setQuantity(event.getRequest().getQuantity());
            o.setPrice(event.getRequest().getPrice());
            o.setTriggerPrice(event.getRequest().getTriggerPrice());
            o.setValidity(event.getRequest().getValidity());
        }
        o.setBrokerType(event.getBrokerType());
        o.setBrokerConfigId(event.getBrokerConfigId());
        o.setPlacedAt(event.getEventTime());
        o.setUpdatedAt(Instant.now());
        return o;
    }

    @Override
    protected String getBusName() { return "orders"; }
}
