package com.fnooms.servlet;

import com.fnooms.broker.BrokerClient;
import com.fnooms.broker.BrokerClientFactory;
import com.fnooms.dao.AlgoKeyValueDAO;
import com.fnooms.util.JsonUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dedicated connectivity check endpoint
 */
public class ConnectivityServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(ConnectivityServlet.class);
    private final AlgoKeyValueDAO dao = new AlgoKeyValueDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String brokerListStr = dao.getValue("web.broker.list");
        if (brokerListStr == null || brokerListStr.trim().isEmpty()) {
            brokerListStr = "MSTOCK"; // fallback
        }
        
        String activeType = dao.getValue("algo.activeBroker");
        if (activeType == null) {
            activeType = "MSTOCK";
        }
        final String finalActiveType = activeType;

        String[] brokers = brokerListStr.split("\\|");

        ExecutorService pool = Executors.newFixedThreadPool(Math.max(1, brokers.length));
        try {
            JsonArray results = new JsonArray();
            List<Future<JsonObject>> futures = new java.util.ArrayList<>();
            for (String broker : brokers) {
                futures.add(pool.submit(() -> testSingle(broker.trim(), finalActiveType)));
            }

            for (int i = 0; i < futures.size(); i++) {
                try {
                    results.add(futures.get(i).get(15, TimeUnit.SECONDS));
                } catch (Exception e) {
                    JsonObject err = new JsonObject();
                    err.addProperty("brokerType", brokers[i].trim());
                    err.addProperty("error", "Test timed out");
                    results.add(err);
                }
            }
            JsonUtil.writeJson(resp, 200, JsonUtil.success(results));
        } finally {
            pool.shutdownNow();
        }
    }

    private JsonObject testSingle(String brokerTypeStr, String activeType) {
        JsonObject result = new JsonObject();
        result.addProperty("brokerType",  brokerTypeStr);
        result.addProperty("displayName", brokerTypeStr);
        
        boolean isActive = brokerTypeStr.equalsIgnoreCase(activeType);
        result.addProperty("active", isActive);
        result.addProperty("isActive", isActive);

        long start = System.currentTimeMillis();
        try {
            BrokerClient client = BrokerClientFactory.getClientFor(brokerTypeStr);
            client.testConnection(); // throws on failure
            long latency = System.currentTimeMillis() - start;

            result.addProperty("connected",  true);
            result.addProperty("latencyMs",  latency);
            result.addProperty("status",     "CONNECTED");
            result.addProperty("error",      (String) null);

            log.info("Connectivity check: broker={} status=OK latency={}ms", brokerTypeStr, latency);

        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            result.addProperty("connected", false);
            result.addProperty("latencyMs", latency);
            result.addProperty("status",    "FAILED");
            result.addProperty("error",     e.getMessage());
            log.warn("Connectivity check failed for broker={}: {}", brokerTypeStr, e.getMessage());
        }

        return result;
    }
}
