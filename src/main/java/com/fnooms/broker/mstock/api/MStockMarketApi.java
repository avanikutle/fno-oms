package com.fnooms.broker.mstock.api;

import com.fnooms.broker.BrokerException;
import com.fnooms.broker.dto.Quote;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles mStock API endpoints for Market Quotes, Instruments, Historical Data, 
 * Intraday Chart Data, Option Chain APIs, and Top Gainers/Losers.
 */
public class MStockMarketApi {

    private static final Logger log = LoggerFactory.getLogger(MStockMarketApi.class);
    private final MStockCoreClient core;

    public MStockMarketApi(MStockCoreClient core) {
        this.core = core;
    }

    public Map<String, Quote> getQuotes(List<String> instruments) throws BrokerException {
        if (instruments == null || instruments.isEmpty()) {
            return Collections.emptyMap();
        }

        StringBuilder url = new StringBuilder(core.getBaseUrl())
                .append("/market-quote/quotes");
        for (int i = 0; i < instruments.size(); i++) {
            url.append(i == 0 ? "?" : "&").append("i=").append(instruments.get(i));
        }

        JsonObject body = core.executeGet(url.toString());
        Map<String, Quote> result = new LinkedHashMap<>();

        JsonObject data = core.safeGetObject(body, "data");
        if (data == null) return result;

        for (Map.Entry<String, JsonElement> entry : data.entrySet()) {
            String key = entry.getKey(); // "NSE:SYMBOL"
            JsonObject q = entry.getValue().getAsJsonObject();
            result.put(key, parseQuote(key, q));
        }

        return result;
    }

    private Quote parseQuote(String key, JsonObject q) {
        Quote quote = new Quote();
        String[] parts = key.split(":", 2);
        quote.setExchange(parts.length > 0 ? parts[0] : "");
        quote.setSymbol(parts.length > 1 ? parts[1] : key);
        quote.setLtp(core.safeDecimal(q, "last_price"));
        quote.setVolume(core.safeLong(q, "volume"));
        quote.setOi(core.safeLong(q, "oi"));
        quote.setOiDayHigh(core.safeLong(q, "oi_day_high"));
        quote.setOiDayLow(core.safeLong(q, "oi_day_low"));
        quote.setBid(core.safeDecimal(q, "depth.buy.0.price"));
        quote.setAsk(core.safeDecimal(q, "depth.sell.0.price"));
        quote.setChange(core.safeDecimal(q, "net_change"));

        if (q.has("ohlc") && !q.get("ohlc").isJsonNull()) {
            JsonObject ohlc = q.getAsJsonObject("ohlc");
            quote.setOpen(core.safeDecimal(ohlc, "open"));
            quote.setHigh(core.safeDecimal(ohlc, "high"));
            quote.setLow(core.safeDecimal(ohlc, "low"));
            quote.setClose(core.safeDecimal(ohlc, "close"));
        }

        if (quote.getClose() != null && quote.getClose().compareTo(BigDecimal.ZERO) != 0
                && quote.getLtp() != null) {
            BigDecimal chg = quote.getLtp().subtract(quote.getClose());
            BigDecimal pct = chg.divide(quote.getClose(), 6, java.math.RoundingMode.HALF_UP)
                               .multiply(BigDecimal.valueOf(100));
            quote.setChange(chg);
            quote.setChangePct(pct);
        }

        quote.setTimestamp(Instant.now());
        return quote;
    }

    // STUBS for other Market APIs
    // public void getInstruments() {}
    // public void getHistoricalData() {}
    // public void getIntradayChartData() {}
    // public void getOptionChain() {}
    // public void getTopGainersLosers() {}
}
