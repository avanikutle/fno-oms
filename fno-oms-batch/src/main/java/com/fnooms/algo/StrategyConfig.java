package com.fnooms.algo;

public class StrategyConfig {
    public enum EntryCondition {
        GREATER_THAN_EQUAL,
        LESS_THAN_EQUAL
    }

    private String symbol;
    private String token;
    
    private double entryPrice;
    private double stopLossPrice;
    private double targetPrice;
    private double trailingSlPoints;
    
    private int quantity;
    private String transactionType; // BUY or SELL
    private String exchange = "NSE"; // NSE or NFO
    private String product = "MIS"; // MIS or NRML
    
    private EntryCondition entryCondition;

    private Long strategyId;

    public Long getStrategyId() { return strategyId; }
    public void setStrategyId(Long strategyId) { this.strategyId = strategyId; }

    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }

    public String getProduct() { return product; }
    public void setProduct(String product) { this.product = product; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public double getEntryPrice() { return entryPrice; }
    public void setEntryPrice(double entryPrice) { this.entryPrice = entryPrice; }

    public double getStopLossPrice() { return stopLossPrice; }
    public void setStopLossPrice(double stopLossPrice) { this.stopLossPrice = stopLossPrice; }

    public double getTargetPrice() { return targetPrice; }
    public void setTargetPrice(double targetPrice) { this.targetPrice = targetPrice; }

    public double getTrailingSlPoints() { return trailingSlPoints; }
    public void setTrailingSlPoints(double trailingSlPoints) { this.trailingSlPoints = trailingSlPoints; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public EntryCondition getEntryCondition() { return entryCondition; }
    public void setEntryCondition(EntryCondition entryCondition) { this.entryCondition = entryCondition; }

    @Override
    public String toString() {
        return "StrategyConfig{" +
                "symbol='" + symbol + '\'' +
                ", token='" + token + '\'' +
                ", entryPrice=" + entryPrice +
                ", stopLossPrice=" + stopLossPrice +
                ", targetPrice=" + targetPrice +
                ", trailingSlPoints=" + trailingSlPoints +
                ", quantity=" + quantity +
                ", transactionType='" + transactionType + '\'' +
                ", entryCondition=" + entryCondition +
                '}';
    }
}
