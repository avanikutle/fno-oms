package com.fnooms.broker.dto;

import java.math.BigDecimal;

/**
 * Represents a holdings entry (delivery/CNC position).
 */
public class Holding {

    private String     symbol;
    private String     exchange;
    private String     isin;
    private int        quantity;
    private int        t1Quantity;       // Shares in T+1 settlement
    private BigDecimal averagePrice;
    private BigDecimal lastPrice;
    private BigDecimal pnl;
    private BigDecimal pnlPct;
    private BigDecimal closePrice;

    public Holding() {}

    // ---- Getters & Setters ----

    public String     getSymbol()               { return symbol; }
    public void       setSymbol(String v)        { this.symbol = v; }

    public String     getExchange()              { return exchange; }
    public void       setExchange(String v)      { this.exchange = v; }

    public String     getIsin()                  { return isin; }
    public void       setIsin(String v)          { this.isin = v; }

    public int        getQuantity()              { return quantity; }
    public void       setQuantity(int v)         { this.quantity = v; }

    public int        getT1Quantity()            { return t1Quantity; }
    public void       setT1Quantity(int v)       { this.t1Quantity = v; }

    public BigDecimal getAveragePrice()          { return averagePrice; }
    public void       setAveragePrice(BigDecimal v){ this.averagePrice = v; }

    public BigDecimal getLastPrice()             { return lastPrice; }
    public void       setLastPrice(BigDecimal v) { this.lastPrice = v; }

    public BigDecimal getPnl()                   { return pnl; }
    public void       setPnl(BigDecimal v)       { this.pnl = v; }

    public BigDecimal getPnlPct()                { return pnlPct; }
    public void       setPnlPct(BigDecimal v)    { this.pnlPct = v; }

    public BigDecimal getClosePrice()            { return closePrice; }
    public void       setClosePrice(BigDecimal v){ this.closePrice = v; }

    public BigDecimal getCurrentValue() {
        if (lastPrice == null || quantity == 0) return BigDecimal.ZERO;
        return lastPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
