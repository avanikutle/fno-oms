package com.fnooms.broker.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Order response returned from broker after placement or status query.
 */
public class OrderResponse {

    private String     brokerOrderId;
    private String     exchangeOrderId;
    private String     symbol;
    private String     exchange;
    private String     transactionType;  // BUY, SELL
    private String     orderType;        // MARKET, LIMIT, SL, SL-M
    private String     product;          // MIS, NRML, CNC
    private int        quantity;
    private int        filledQuantity;
    private int        pendingQuantity;
    private BigDecimal price;
    private BigDecimal triggerPrice;
    private BigDecimal averagePrice;
    private String     status;           // OPEN, COMPLETE, CANCELLED, REJECTED, TRIGGER PENDING
    private String     statusMessage;
    private String     validity;
    private String     tag;
    private Instant    orderTimestamp;
    private Instant    exchangeTimestamp;

    public OrderResponse() {}

    // ---- Getters & Setters ----

    public String  getBrokerOrderId()              { return brokerOrderId; }
    public void    setBrokerOrderId(String v)      { this.brokerOrderId = v; }

    public String  getExchangeOrderId()            { return exchangeOrderId; }
    public void    setExchangeOrderId(String v)    { this.exchangeOrderId = v; }

    public String  getSymbol()                     { return symbol; }
    public void    setSymbol(String v)             { this.symbol = v; }

    public String  getExchange()                   { return exchange; }
    public void    setExchange(String v)           { this.exchange = v; }

    public String  getTransactionType()            { return transactionType; }
    public void    setTransactionType(String v)    { this.transactionType = v; }

    public String  getOrderType()                  { return orderType; }
    public void    setOrderType(String v)          { this.orderType = v; }

    public String  getProduct()                    { return product; }
    public void    setProduct(String v)            { this.product = v; }

    public int     getQuantity()                   { return quantity; }
    public void    setQuantity(int v)              { this.quantity = v; }

    public int     getFilledQuantity()             { return filledQuantity; }
    public void    setFilledQuantity(int v)        { this.filledQuantity = v; }

    public int     getPendingQuantity()            { return pendingQuantity; }
    public void    setPendingQuantity(int v)       { this.pendingQuantity = v; }

    public BigDecimal getPrice()                   { return price; }
    public void       setPrice(BigDecimal v)       { this.price = v; }

    public BigDecimal getTriggerPrice()            { return triggerPrice; }
    public void       setTriggerPrice(BigDecimal v){ this.triggerPrice = v; }

    public BigDecimal getAveragePrice()            { return averagePrice; }
    public void       setAveragePrice(BigDecimal v){ this.averagePrice = v; }

    public String  getStatus()                     { return status; }
    public void    setStatus(String v)             { this.status = v; }

    public String  getStatusMessage()              { return statusMessage; }
    public void    setStatusMessage(String v)      { this.statusMessage = v; }

    public String  getValidity()                   { return validity; }
    public void    setValidity(String v)           { this.validity = v; }

    public String  getTag()                        { return tag; }
    public void    setTag(String v)                { this.tag = v; }

    public Instant getOrderTimestamp()             { return orderTimestamp; }
    public void    setOrderTimestamp(Instant v)    { this.orderTimestamp = v; }

    public Instant getExchangeTimestamp()          { return exchangeTimestamp; }
    public void    setExchangeTimestamp(Instant v) { this.exchangeTimestamp = v; }

    public boolean isComplete()  { return "COMPLETE".equalsIgnoreCase(status); }
    public boolean isRejected()  { return "REJECTED".equalsIgnoreCase(status); }
    public boolean isCancelled() { return "CANCELLED".equalsIgnoreCase(status); }
    public boolean isOpen()      { return "OPEN".equalsIgnoreCase(status); }

    @Override
    public String toString() {
        return "OrderResponse{id=" + brokerOrderId + " " + transactionType + " " + quantity
                + " " + symbol + " status=" + status + "}";
    }
}
