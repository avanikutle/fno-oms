package com.fnooms.model.dto;

import java.math.BigDecimal;

public class GrowwHoldingDTO {
    private String isin;
    private String trading_symbol;
    private int quantity;
    private BigDecimal average_price;

    public String getIsin() { return isin; }
    public void setIsin(String isin) { this.isin = isin; }

    public String getTrading_symbol() { return trading_symbol; }
    public void setTrading_symbol(String trading_symbol) { this.trading_symbol = trading_symbol; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public BigDecimal getAverage_price() { return average_price; }
    public void setAverage_price(BigDecimal average_price) { this.average_price = average_price; }
}
