package com.fnooms.async.event;

import com.fnooms.broker.dto.OrderRequest;
import com.fnooms.broker.dto.OrderResponse;

import java.time.Instant;

/**
 * Event published to the OrderEventBus after an order is placed/updated.
 * Carries everything needed for the DB writer to persist the order record.
 */
public class OrderEvent {

    public enum Type { PLACED, UPDATED, CANCELLED }

    private final Type          type;
    private final OrderRequest  request;
    private final OrderResponse response;
    private final int           brokerConfigId;
    private final Instant       eventTime;

    public OrderEvent(Type type, OrderRequest request, OrderResponse response, int brokerConfigId) {
        this.type           = type;
        this.request        = request;
        this.response       = response;
        this.brokerConfigId = brokerConfigId;
        this.eventTime      = Instant.now();
    }

    public Type          getType()           { return type; }
    public OrderRequest  getRequest()        { return request; }
    public OrderResponse getResponse()       { return response; }
    public int           getBrokerConfigId() { return brokerConfigId; }
    public Instant       getEventTime()      { return eventTime; }
}
