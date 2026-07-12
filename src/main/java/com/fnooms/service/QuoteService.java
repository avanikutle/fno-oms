package com.fnooms.service;

import com.fnooms.async.AuditEventBus;
import com.fnooms.async.TickEventBus;
import com.fnooms.async.event.AuditEvent;
import com.fnooms.async.event.TickEvent;
import com.fnooms.broker.BrokerClient;
import com.fnooms.broker.BrokerClientFactory;
import com.fnooms.broker.BrokerException;
import com.fnooms.broker.dto.Quote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Fetches live quotes from the active broker.
 * Optionally persists ticks to TimescaleDB via TickEventBus.
 */
public class QuoteService {

    private static final Logger log = LoggerFactory.getLogger(QuoteService.class);

    /**
     * Fetch live quotes for the given instruments.
     *
     * @param instruments List of "EXCHANGE:SYMBOL" strings
     * @param persistTicks If true, publish tick events for TimescaleDB storage
     */
    public Map<String, Quote> getQuotes(List<String> instruments, boolean persistTicks)
            throws BrokerException {

        BrokerClient client = BrokerClientFactory.getActiveClient();
        long start = System.currentTimeMillis();

        Map<String, Quote> quotes = client.getQuotes(instruments);
        long latency = System.currentTimeMillis() - start;

        // Async audit
        AuditEventBus.getInstance().publish(
                new AuditEvent("GET_QUOTES", client.getBrokerType().name(),
                        "/market-quote/quotes",
                        "symbols=" + instruments,
                        "count=" + quotes.size(),
                        200, latency, null));

        // Optionally persist ticks asynchronously
        if (persistTicks) {
            quotes.values().forEach(q -> TickEventBus.getInstance().publish(
                    new TickEvent(q.getSymbol(), q.getExchange(),
                            q.getLtp(), q.getOpen(), q.getHigh(), q.getLow(), q.getClose(),
                            q.getBid(), q.getAsk(), q.getVolume(), q.getOi(),
                            q.getChange(), q.getChangePct(),
                            client.getBrokerType().name())));
        }

        return quotes;
    }
}
