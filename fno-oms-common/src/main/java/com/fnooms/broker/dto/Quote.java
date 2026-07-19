package com.fnooms.broker.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Live market quote for a single instrument.
 */
public class Quote {

    private String symbol;
    private String exchange;
    private BigDecimal ltp;          // Last Traded Price
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;        // Previous close
    private BigDecimal bid;
    private BigDecimal ask;
    private long      volume;
    private long      oi;            // Open Interest
    private long      oiDayHigh;
    private long      oiDayLow;
    private BigDecimal change;       // ltp - close
    private BigDecimal changePct;    // % change from close
    private Instant   timestamp;

    public Quote() {}

    // ---- Getters & Setters ----

    public String getSymbol()          { return symbol; }
    public void   setSymbol(String v)  { this.symbol = v; }

    public String getExchange()           { return exchange; }
    public void   setExchange(String v)   { this.exchange = v; }

    public BigDecimal getLtp()            { return ltp; }
    public void       setLtp(BigDecimal v){ this.ltp = v; }

    public BigDecimal getOpen()           { return open; }
    public void       setOpen(BigDecimal v){ this.open = v; }

    public BigDecimal getHigh()           { return high; }
    public void       setHigh(BigDecimal v){ this.high = v; }

    public BigDecimal getLow()            { return low; }
    public void       setLow(BigDecimal v) { this.low = v; }

    public BigDecimal getClose()           { return close; }
    public void       setClose(BigDecimal v){ this.close = v; }

    public BigDecimal getBid()             { return bid; }
    public void       setBid(BigDecimal v) { this.bid = v; }

    public BigDecimal getAsk()             { return ask; }
    public void       setAsk(BigDecimal v) { this.ask = v; }

    public long   getVolume()              { return volume; }
    public void   setVolume(long v)        { this.volume = v; }

    public long   getOi()                  { return oi; }
    public void   setOi(long v)            { this.oi = v; }

    public long   getOiDayHigh()           { return oiDayHigh; }
    public void   setOiDayHigh(long v)     { this.oiDayHigh = v; }

    public long   getOiDayLow()            { return oiDayLow; }
    public void   setOiDayLow(long v)      { this.oiDayLow = v; }

    public BigDecimal getChange()           { return change; }
    public void       setChange(BigDecimal v){ this.change = v; }

    public BigDecimal getChangePct()           { return changePct; }
    public void       setChangePct(BigDecimal v){ this.changePct = v; }

    public Instant  getTimestamp()         { return timestamp; }
    public void     setTimestamp(Instant v){ this.timestamp = v; }

    @Override
    public String toString() {
        return "Quote{" + exchange + ":" + symbol + " ltp=" + ltp + " oi=" + oi + "}";
    }
}
