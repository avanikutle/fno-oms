package com.fnooms.broker;

import com.fnooms.broker.dto.*;
import java.util.List;
import java.util.Map;

/**
 * SPI (Service Provider Interface) for broker integrations.
 *
 * <p>To add a new broker:
 * <ol>
 *   <li>Implement this interface in a new class under broker/{broker_name}/</li>
 *   <li>Add a new entry to {@link BrokerType}</li>
 *   <li>Register the implementation in {@link BrokerClientFactory}</li>
 * </ol>
 *
 * <p>All methods are synchronous — the caller decides whether to run them
 * on a dedicated thread or inline. DB persistence is always done via
 * the async event bus layer, never inside these methods.
 */
public interface BrokerClient {

    /** @return The broker type this client handles. */
    BrokerType getBrokerType();

    /**
     * Fetch live quotes for one or more instruments.
     *
     * @param instruments List of "EXCHANGE:SYMBOL" strings, e.g. ["NFO:NIFTY24JUL24000CE"]
     * @return Map keyed by "EXCHANGE:SYMBOL" → Quote
     */
    Map<String, Quote> getQuotes(List<String> instruments) throws BrokerException;

    /**
     * Place an order with the broker.
     *
     * @param request  Order details
     * @return Broker's acknowledgment with order ID and initial status
     */
    OrderResponse placeOrder(OrderRequest request) throws BrokerException;

    /**
     * Fetch the full order book for today.
     *
     * @return List of all orders placed today
     */
    List<OrderResponse> getOrderBook() throws BrokerException;

    /**
     * Cancel an open order.
     *
     * @param brokerOrderId The broker-assigned order ID
     * @return true if cancellation was accepted
     */
    boolean cancelOrder(String brokerOrderId) throws BrokerException;

    /**
     * Modify an existing open order.
     *
     * @param brokerOrderId Existing order ID
     * @param updated       New order parameters (only changed fields need to be set)
     * @return Updated order response
     */
    OrderResponse modifyOrder(String brokerOrderId, OrderRequest updated) throws BrokerException;

    /**
     * Fetch net positions (intraday + overnight).
     */
    List<Position> getPositions() throws BrokerException;

    /**
     * Fetch delivery holdings (CNC positions).
     */
    List<Holding> getHoldings() throws BrokerException;

    /**
     * Validate that the stored credentials are working.
     *
     * @return true if a test API call succeeds
     */
    boolean testConnection();
}
