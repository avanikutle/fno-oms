package com.fnooms.servlet;

import com.fnooms.broker.BrokerClient;
import com.fnooms.broker.BrokerClientFactory;
import com.fnooms.broker.BrokerType;
import com.fnooms.dao.AlgoKeyValueDAO;
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
import java.util.concurrent.*;

/**
 * Dedicated connectivity check endpoint
 */
public class ConnectivityServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(ConnectivityServlet.class);
    private final AlgoKeyValueDAO dao = new AlgoKeyValueDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // For now, only test MSTOCK
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<JsonObject> future = pool.submit(() -> testSingle("MSTOCK"));
            JsonArray results = new JsonArray();
            try {
                results.add(future.get(15, TimeUnit.SECONDS));
            } catch (Exception e) {
                JsonObject err = new JsonObject();
                err.addProperty("error", "Test timed out");
                results.add(err);
            }
            JsonUtil.writeJson(resp, 200, JsonUtil.success(results));
        } finally {
            pool.shutdownNow();
        }
    }

    private JsonObject testSingle(String brokerTypeStr) {
        JsonObject result = new JsonObject();
        result.addProperty("brokerType",  brokerTypeStr);
        result.addProperty("displayName", brokerTypeStr);

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
