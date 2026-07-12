package com.fnooms.async.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event published to the TickEventBus for each live quote snapshot.
 * Batched and bulk-inserted into the quote_ticks TimescaleDB hypertable.
 */
public class TickEvent {

    private final Instant    time;
    private final String     symbol;
    private final String     exchange;
    private final BigDecimal ltp;
    private final BigDecimal open;
    private final BigDecimal high;
    private final BigDecimal low;
    private final BigDecimal close;
    private final BigDecimal bid;
    private final BigDecimal ask;
    private final long       volume;
    private final long       oi;
    private final BigDecimal change;
    private final BigDecimal changePct;
    private final String     brokerType;

    public TickEvent(String symbol, String exchange, BigDecimal ltp,
                     BigDecimal open, BigDecimal high, BigDecimal low,
                     BigDecimal close, BigDecimal bid, BigDecimal ask,
                     long volume, long oi, BigDecimal change, BigDecimal changePct,
                     String brokerType) {
        this.time       = Instant.now();
        this.symbol     = symbol;
        this.exchange   = exchange;
        this.ltp        = ltp;
        this.open       = open;
        this.high       = high;
        this.low        = low;
        this.close      = close;
        this.bid        = bid;
        this.ask        = ask;
        this.volume     = volume;
        this.oi         = oi;
        this.change     = change;
        this.changePct  = changePct;
        this.brokerType = brokerType;
    }

    public Instant    getTime()       { return time; }
    public String     getSymbol()     { return symbol; }
    public String     getExchange()   { return exchange; }
    public BigDecimal getLtp()        { return ltp; }
    public BigDecimal getOpen()       { return open; }
    public BigDecimal getHigh()       { return high; }
    public BigDecimal getLow()        { return low; }
    public BigDecimal getClose()      { return close; }
    public BigDecimal getBid()        { return bid; }
    public BigDecimal getAsk()        { return ask; }
    public long       getVolume()     { return volume; }
    public long       getOi()         { return oi; }
    public BigDecimal getChange()     { return change; }
    public BigDecimal getChangePct()  { return changePct; }
    public String     getBrokerType() { return brokerType; }
}
