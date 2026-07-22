package com.fnooms.model;

import java.math.BigDecimal;

/**
 * A unified model for a holding, aggregating common fields from multiple brokers.
 */
public class StandardHolding {
    private String isin;
    private String tradingSymbol;
    private int quantity;
    private BigDecimal averagePrice;

    public String getIsin() { return isin; }
    public void setIsin(String isin) { this.isin = isin; }

    public String getTradingSymbol() { return tradingSymbol; }
    public void setTradingSymbol(String tradingSymbol) { this.tradingSymbol = tradingSymbol; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public BigDecimal getAveragePrice() { return averagePrice; }
    public void setAveragePrice(BigDecimal averagePrice) { this.averagePrice = averagePrice; }
    
    @Override
    public String toString() {
        return "StandardHolding{" +
                "isin='" + isin + '\'' +
                ", tradingSymbol='" + tradingSymbol + '\'' +
                ", quantity=" + quantity +
                ", averagePrice=" + averagePrice +
                '}';
    }
}
