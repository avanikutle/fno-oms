package com.fnooms.broker.dto;

import java.math.BigDecimal;

/**
 * Order placement request — broker-agnostic.
 * Maps to whichever active broker's API format.
 */
public class OrderRequest {

    private String     symbol;          // e.g. NIFTY24JUL24000CE
    private String     exchange;        // NSE, NFO, BSE, BFO
    private String     transactionType; // BUY, SELL
    private String     orderType;       // MARKET, LIMIT, SL, SL-M
    private String     product;         // MIS (intraday), NRML (overnight), CNC (delivery)
    private int        quantity;
    private BigDecimal price;           // Required for LIMIT, SL
    private BigDecimal triggerPrice;    // Required for SL, SL-M
    private String     validity;        // DAY, IOC, GTT
    private String     tag;             // Optional user tag/label for this order
    private boolean    isAmo;           // After Market Order flag

    public OrderRequest() {
        this.validity = "DAY";
        this.isAmo = false;
    }

    // ---- Builder-style setters for fluent use ----

    public OrderRequest symbol(String v)          { this.symbol = v; return this; }
    public OrderRequest exchange(String v)         { this.exchange = v; return this; }
    public OrderRequest buy()                      { this.transactionType = "BUY"; return this; }
    public OrderRequest sell()                     { this.transactionType = "SELL"; return this; }
    public OrderRequest market()                   { this.orderType = "MARKET"; return this; }
    public OrderRequest limit(BigDecimal price)    { this.orderType = "LIMIT"; this.price = price; return this; }
    public OrderRequest sl(BigDecimal p, BigDecimal tp){ this.orderType = "SL"; this.price = p; this.triggerPrice = tp; return this; }
    public OrderRequest slm(BigDecimal tp)         { this.orderType = "SL-M"; this.triggerPrice = tp; return this; }
    public OrderRequest quantity(int v)            { this.quantity = v; return this; }
    public OrderRequest product(String v)          { this.product = v; return this; }
    public OrderRequest validity(String v)         { this.validity = v; return this; }
    public OrderRequest tag(String v)              { this.tag = v; return this; }
    public OrderRequest amo()                      { this.isAmo = true; return this; }

    // ---- Standard Getters ----

    public String     getSymbol()          { return symbol; }
    public String     getExchange()        { return exchange; }
    public String     getTransactionType() { return transactionType; }
    public String     getOrderType()       { return orderType; }
    public String     getProduct()         { return product; }
    public int        getQuantity()        { return quantity; }
    public BigDecimal getPrice()           { return price; }
    public BigDecimal getTriggerPrice()    { return triggerPrice; }
    public String     getValidity()        { return validity; }
    public String     getTag()             { return tag; }
    public boolean    isAmo()              { return isAmo; }

    // ---- Standard Setters ----
    public void setSymbol(String symbol)                 { this.symbol = symbol; }
    public void setExchange(String exchange)             { this.exchange = exchange; }
    public void setTransactionType(String t)             { this.transactionType = t; }
    public void setOrderType(String orderType)           { this.orderType = orderType; }
    public void setProduct(String product)               { this.product = product; }
    public void setQuantity(int quantity)                { this.quantity = quantity; }
    public void setPrice(BigDecimal price)               { this.price = price; }
    public void setTriggerPrice(BigDecimal triggerPrice) { this.triggerPrice = triggerPrice; }
    public void setValidity(String validity)             { this.validity = validity; }
    public void setTag(String tag)                       { this.tag = tag; }
    public void setAmo(boolean amo)                      { this.isAmo = amo; }

    @Override
    public String toString() {
        return "OrderRequest{" + transactionType + " " + quantity + " " + exchange + ":" + symbol
                + " " + orderType + "@" + price + " prod=" + product + "}";
    }
}
