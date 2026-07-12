package com.fnooms.servlet;

import com.fnooms.broker.BrokerException;
import com.fnooms.service.PortfolioService;
import com.fnooms.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * REST endpoint for portfolio data.
 *
 * GET /api/portfolio/positions  → net positions from broker
 * GET /api/portfolio/holdings   → delivery holdings from broker
 */
public class PortfolioServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(PortfolioServlet.class);
    private final PortfolioService portfolioService = new PortfolioService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        try {
            if ("/holdings".equals(path)) {
                JsonUtil.writeJson(resp, 200,
                        JsonUtil.success(portfolioService.getHoldings()));
            } else {
                // default: /positions or bare /api/portfolio
                JsonUtil.writeJson(resp, 200,
                        JsonUtil.success(portfolioService.getPositions()));
            }
        } catch (IllegalStateException e) {
            JsonUtil.writeJson(resp, 503, JsonUtil.error(e.getMessage()));
        } catch (BrokerException e) {
            log.warn("Portfolio request failed: {}", e.getMessage());
            JsonUtil.writeJson(resp, 502, JsonUtil.error(e.getMessage()));
        }
    }
}
