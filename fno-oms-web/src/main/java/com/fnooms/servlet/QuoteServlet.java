package com.fnooms.servlet;

import com.fnooms.broker.BrokerException;
import com.fnooms.broker.dto.Quote;
import com.fnooms.service.QuoteService;
import com.fnooms.util.JsonUtil;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST endpoint for live market quotes.
 *
 * GET /api/quote?symbols=NFO:NIFTY24JUL24000CE,NSE:NIFTY50&persist=true
 *
 * Called every 2 seconds by quotes.js for the live ticker.
 * The persist=true flag also pushes ticks to TimescaleDB.
 */
public class QuoteServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(QuoteServlet.class);
    private final QuoteService quoteService = new QuoteService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String symbolsParam = req.getParameter("symbols");
        if (symbolsParam == null || symbolsParam.isBlank()) {
            JsonUtil.writeJson(resp, 400, JsonUtil.error("symbols parameter required (comma-separated EXCHANGE:SYMBOL)"));
            return;
        }

        List<String> instruments = Arrays.stream(symbolsParam.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        boolean persist = "true".equalsIgnoreCase(req.getParameter("persist"));

        try {
            Map<String, Quote> quotes = quoteService.getQuotes(instruments, persist);
            JsonUtil.writeJson(resp, 200, JsonUtil.success(quotes));
        } catch (IllegalStateException e) {
            JsonUtil.writeJson(resp, 503, JsonUtil.error(e.getMessage()));
        } catch (BrokerException e) {
            if (e.getMessage() != null && e.getMessage().contains("404")) {
                log.warn("GetQuotes 404 for {}: {}", instruments, e.getMessage());
                JsonUtil.writeJson(resp, 200, JsonUtil.success(Collections.emptyMap()));
            } else {
                log.warn("GetQuotes failed for {}: {}", instruments, e.getMessage());
                JsonUtil.writeJson(resp, 502, JsonUtil.error(e.getMessage()));
            }
        }
    }
}
