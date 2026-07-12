package com.fnooms.servlet;

import com.fnooms.broker.BrokerClient;
import com.fnooms.broker.BrokerClientFactory;
import com.fnooms.broker.BrokerType;
import com.fnooms.dao.BrokerConfigDAO;
import com.fnooms.model.BrokerConfig;
import com.fnooms.util.JsonUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;

/**
 * Dedicated connectivity check endpoint — tests every configured broker
 * in parallel and returns latency + status for each.
 *
 * GET  /api/connectivity          → test all configured brokers
 * GET  /api/connectivity/{id}     → test a specific broker by config ID
 *
 * Used exclusively by the Connectivity Check screen.
 */
public class ConnectivityServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(ConnectivityServlet.class);
    private final BrokerConfigDAO dao = new BrokerConfigDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();

        if (path != null && path.length() > 1) {
            // Test a single specific broker config
            try {
                int id = Integer.parseInt(path.substring(1));
                BrokerConfig cfg = dao.findById(id);
                if (cfg == null) {
                    JsonUtil.writeJson(resp, 404, JsonUtil.error("Broker config not found"));
                    return;
                }
                JsonUtil.writeJson(resp, 200, JsonUtil.success(testSingle(cfg)));
            } catch (NumberFormatException e) {
                JsonUtil.writeJson(resp, 400, JsonUtil.error("Invalid ID"));
            }
        } else {
            // Test all configured brokers in parallel
            List<BrokerConfig> configs = dao.findAll();
            if (configs.isEmpty()) {
                JsonUtil.writeJson(resp, 200, JsonUtil.success(new JsonArray()));
                return;
            }

            ExecutorService pool = Executors.newFixedThreadPool(
                    Math.min(configs.size(), 5));
            try {
                List<Future<JsonObject>> futures = configs.stream()
                        .map(cfg -> pool.submit(() -> testSingle(cfg)))
                        .toList();

                JsonArray results = new JsonArray();
                for (Future<JsonObject> f : futures) {
                    try {
                        results.add(f.get(15, TimeUnit.SECONDS));
                    } catch (Exception e) {
                        JsonObject err = new JsonObject();
                        err.addProperty("error", "Test timed out");
                        results.add(err);
                    }
                }
                JsonUtil.writeJson(resp, 200, JsonUtil.success(results));
            } finally {
                pool.shutdownNow();
            }
        }
    }

    /**
     * Pings a single broker and returns a connectivity result object.
     */
    private JsonObject testSingle(BrokerConfig cfg) {
        JsonObject result = new JsonObject();
        result.addProperty("id",          cfg.getId());
        result.addProperty("brokerType",  cfg.getBrokerType());
        result.addProperty("displayName", cfg.getDisplayName());
        result.addProperty("isActive",    cfg.isActive());
        result.addProperty("tokenExpired", cfg.isTokenExpired());

        if (!cfg.isTokenExpired() && cfg.getApiKey() != null && cfg.getAccessToken() != null) {
            long start = System.currentTimeMillis();
            try {
                BrokerClient client = BrokerClientFactory.getClientFor(cfg);
                client.testConnection(); // throws on failure
                long latency = System.currentTimeMillis() - start;

                result.addProperty("connected",  true);
                result.addProperty("latencyMs",  latency);
                result.addProperty("status",     "CONNECTED");
                result.addProperty("error",      (String) null);

                log.info("Connectivity check: broker={} status=OK latency={}ms",
                        cfg.getBrokerType(), latency);

            } catch (Exception e) {
                long latency = System.currentTimeMillis() - start;
                result.addProperty("connected", false);
                result.addProperty("latencyMs", latency);
                result.addProperty("status",    "FAILED");
                result.addProperty("error",     e.getMessage());
                log.warn("Connectivity check failed for broker={}: {}", cfg.getBrokerType(), e.getMessage());
            }
        } else {
            result.addProperty("connected",  false);
            result.addProperty("latencyMs",  -1);
            result.addProperty("status",     "NOT_CONFIGURED");
            result.addProperty("error",
                    cfg.isTokenExpired() ? "Access token expired — please update it in Settings"
                                        : "API key or access token not set");
        }

        return result;
    }
}
