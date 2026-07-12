package com.fnooms.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Local DB representation of an order (mirrors broker + local metadata).
 */
public class Order {

    private Long      id;
    private String    brokerOrderId;
    private String    brokerType;
    private Integer   brokerConfigId;
    private String    symbol;
    private String    exchange;
    private String    transactionType;
    private String    orderType;
    private String    product;
    private int       quantity;
    private BigDecimal price;
    private BigDecimal triggerPrice;
    private String    validity;
    private String    status;
    private String    statusMessage;
    private int       filledQuantity;
    private BigDecimal averagePrice;
    private String    exchangeOrderId;
    private Instant   exchangeTimestamp;
    private Instant   placedAt;
    private Instant   updatedAt;

    public Order() {}

    // ---- Getters & Setters ----
    public Long      getId()                        { return id; }
    public void      setId(Long v)                  { this.id = v; }

    public String    getBrokerOrderId()             { return brokerOrderId; }
    public void      setBrokerOrderId(String v)     { this.brokerOrderId = v; }

    public String    getBrokerType()                { return brokerType; }
    public void      setBrokerType(String v)        { this.brokerType = v; }

    public Integer   getBrokerConfigId()            { return brokerConfigId; }
    public void      setBrokerConfigId(Integer v)   { this.brokerConfigId = v; }

    public String    getSymbol()                    { return symbol; }
    public void      setSymbol(String v)            { this.symbol = v; }

    public String    getExchange()                  { return exchange; }
    public void      setExchange(String v)          { this.exchange = v; }

    public String    getTransactionType()           { return transactionType; }
    public void      setTransactionType(String v)   { this.transactionType = v; }

    public String    getOrderType()                 { return orderType; }
    public void      setOrderType(String v)         { this.orderType = v; }

    public String    getProduct()                   { return product; }
    public void      setProduct(String v)           { this.product = v; }

    public int       getQuantity()                  { return quantity; }
    public void      setQuantity(int v)             { this.quantity = v; }

    public BigDecimal getPrice()                    { return price; }
    public void       setPrice(BigDecimal v)        { this.price = v; }

    public BigDecimal getTriggerPrice()             { return triggerPrice; }
    public void       setTriggerPrice(BigDecimal v) { this.triggerPrice = v; }

    public String    getValidity()                  { return validity; }
    public void      setValidity(String v)          { this.validity = v; }

    public String    getStatus()                    { return status; }
    public void      setStatus(String v)            { this.status = v; }

    public String    getStatusMessage()             { return statusMessage; }
    public void      setStatusMessage(String v)     { this.statusMessage = v; }

    public int       getFilledQuantity()            { return filledQuantity; }
    public void      setFilledQuantity(int v)       { this.filledQuantity = v; }

    public BigDecimal getAveragePrice()             { return averagePrice; }
    public void       setAveragePrice(BigDecimal v) { this.averagePrice = v; }

    public String    getExchangeOrderId()           { return exchangeOrderId; }
    public void      setExchangeOrderId(String v)   { this.exchangeOrderId = v; }

    public Instant   getExchangeTimestamp()         { return exchangeTimestamp; }
    public void      setExchangeTimestamp(Instant v){ this.exchangeTimestamp = v; }

    public Instant   getPlacedAt()                  { return placedAt; }
    public void      setPlacedAt(Instant v)         { this.placedAt = v; }

    public Instant   getUpdatedAt()                 { return updatedAt; }
    public void      setUpdatedAt(Instant v)        { this.updatedAt = v; }
}
