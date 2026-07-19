package com.fnooms.broker.dto;

import java.math.BigDecimal;

/**
 * Represents an open/closed position in the portfolio.
 */
public class Position {

    private String     symbol;
    private String     exchange;
    private String     product;          // MIS, NRML
    private int        quantity;         // Net quantity (positive = long, negative = short)
    private int        overnightQty;
    private int        dayQty;
    private BigDecimal averagePrice;
    private BigDecimal lastPrice;
    private BigDecimal pnl;              // Unrealised P&L
    private BigDecimal realisedPnl;
    private BigDecimal unrealisedPnl;
    private BigDecimal buyPrice;
    private BigDecimal sellPrice;
    private int        buyQuantity;
    private int        sellQuantity;
    private BigDecimal buyValue;
    private BigDecimal sellValue;
    private BigDecimal multiplier;       // Lot size multiplier for F&O
    private boolean    isNetPosition;

    public Position() {}

    // ---- Getters & Setters ----

    public String     getSymbol()               { return symbol; }
    public void       setSymbol(String v)        { this.symbol = v; }

    public String     getExchange()              { return exchange; }
    public void       setExchange(String v)      { this.exchange = v; }

    public String     getProduct()               { return product; }
    public void       setProduct(String v)       { this.product = v; }

    public int        getQuantity()              { return quantity; }
    public void       setQuantity(int v)         { this.quantity = v; }

    public int        getOvernightQty()          { return overnightQty; }
    public void       setOvernightQty(int v)     { this.overnightQty = v; }

    public int        getDayQty()                { return dayQty; }
    public void       setDayQty(int v)           { this.dayQty = v; }

    public BigDecimal getAveragePrice()          { return averagePrice; }
    public void       setAveragePrice(BigDecimal v){ this.averagePrice = v; }

    public BigDecimal getLastPrice()             { return lastPrice; }
    public void       setLastPrice(BigDecimal v) { this.lastPrice = v; }

    public BigDecimal getPnl()                   { return pnl; }
    public void       setPnl(BigDecimal v)       { this.pnl = v; }

    public BigDecimal getRealisedPnl()           { return realisedPnl; }
    public void       setRealisedPnl(BigDecimal v){ this.realisedPnl = v; }

    public BigDecimal getUnrealisedPnl()         { return unrealisedPnl; }
    public void       setUnrealisedPnl(BigDecimal v){ this.unrealisedPnl = v; }

    public BigDecimal getBuyPrice()              { return buyPrice; }
    public void       setBuyPrice(BigDecimal v)  { this.buyPrice = v; }

    public BigDecimal getSellPrice()             { return sellPrice; }
    public void       setSellPrice(BigDecimal v) { this.sellPrice = v; }

    public int        getBuyQuantity()           { return buyQuantity; }
    public void       setBuyQuantity(int v)      { this.buyQuantity = v; }

    public int        getSellQuantity()          { return sellQuantity; }
    public void       setSellQuantity(int v)     { this.sellQuantity = v; }

    public BigDecimal getBuyValue()              { return buyValue; }
    public void       setBuyValue(BigDecimal v)  { this.buyValue = v; }

    public BigDecimal getSellValue()             { return sellValue; }
    public void       setSellValue(BigDecimal v) { this.sellValue = v; }

    public BigDecimal getMultiplier()            { return multiplier; }
    public void       setMultiplier(BigDecimal v){ this.multiplier = v; }

    public boolean    isNetPosition()            { return isNetPosition; }
    public void       setNetPosition(boolean v)  { this.isNetPosition = v; }

    public boolean    isLong()  { return quantity > 0; }
    public boolean    isShort() { return quantity < 0; }
}
