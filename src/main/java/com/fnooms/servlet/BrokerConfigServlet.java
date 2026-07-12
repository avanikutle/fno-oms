package com.fnooms.servlet;

import com.fnooms.broker.BrokerClient;
import com.fnooms.broker.BrokerClientFactory;
import com.fnooms.broker.BrokerType;
import com.fnooms.dao.BrokerConfigDAO;
import com.fnooms.model.BrokerConfig;
import com.fnooms.util.JsonUtil;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

/**
 * REST endpoint for broker configuration management.
 *
 * GET    /api/broker-config           → list all configured brokers
 * POST   /api/broker-config           → add a new broker config
 * PUT    /api/broker-config/{id}      → update credentials
 * PUT    /api/broker-config/{id}/activate → set as active broker
 * DELETE /api/broker-config/{id}      → remove a broker
 */
public class BrokerConfigServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(BrokerConfigServlet.class);
    private final BrokerConfigDAO dao = new BrokerConfigDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        List<BrokerConfig> configs = dao.findAll();
        // Mask credentials before sending to frontend
        configs.forEach(c -> {
            c.setApiKey(c.getMaskedApiKey());
            c.setPrivateKey("****");
            c.setAccessToken(c.getMaskedToken());
        });
        JsonUtil.writeJson(resp, 200, JsonUtil.success(configs));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String body = new String(req.getInputStream().readAllBytes());
            JsonObject json = JsonUtil.parseObject(body);

            BrokerConfig cfg = new BrokerConfig();
            cfg.setBrokerType(requireString(json, "brokerType"));
            cfg.setDisplayName(requireString(json, "displayName"));
            cfg.setApiKey(requireString(json, "apiKey"));
            cfg.setPrivateKey(getString(json, "privateKey"));
            cfg.setAccessToken(requireString(json, "accessToken"));
            cfg.setClientId(getString(json, "clientId"));
            cfg.setActive(false);

            // Token expires at midnight today
            cfg.setTokenExpiry(getMidnightToday());

            // Validate broker type is recognised
            BrokerType.fromString(cfg.getBrokerType());

            int id = dao.insert(cfg);
            if (id < 0) throw new RuntimeException("Insert failed");

            JsonObject result = new JsonObject();
            result.addProperty("id", id);
            JsonUtil.writeJson(resp, 200, JsonUtil.success(result));

        } catch (IllegalArgumentException e) {
            JsonUtil.writeJson(resp, 400, JsonUtil.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Add broker config failed", e);
            JsonUtil.writeJson(resp, 500, JsonUtil.error("Failed to save broker config"));
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo(); // e.g. "/5" or "/5/activate"
        if (path == null || path.equals("/")) {
            JsonUtil.writeJson(resp, 400, JsonUtil.error("ID required"));
            return;
        }

        String[] parts = path.split("/");
        int id;
        try {
            id = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            JsonUtil.writeJson(resp, 400, JsonUtil.error("Invalid ID"));
            return;
        }

        // /api/broker-config/{id}/activate
        if (parts.length > 2 && "activate".equals(parts[2])) {
            dao.setActive(id);
            JsonUtil.writeJson(resp, 200, JsonUtil.success("Broker activated"));
            return;
        }

        // Update credentials
        try {
            String body = new String(req.getInputStream().readAllBytes());
            JsonObject json = JsonUtil.parseObject(body);

            BrokerConfig existing = dao.findById(id);
            if (existing == null) {
                JsonUtil.writeJson(resp, 404, JsonUtil.error("Broker config not found"));
                return;
            }

            if (json.has("displayName"))  existing.setDisplayName(json.get("displayName").getAsString());
            if (json.has("apiKey"))       existing.setApiKey(json.get("apiKey").getAsString());
            if (json.has("privateKey"))   existing.setPrivateKey(json.get("privateKey").getAsString());
            if (json.has("accessToken")) {
                existing.setAccessToken(json.get("accessToken").getAsString());
                existing.setTokenExpiry(getMidnightToday());
            }
            if (json.has("clientId"))     existing.setClientId(json.get("clientId").getAsString());

            dao.update(existing);
            JsonUtil.writeJson(resp, 200, JsonUtil.success("Updated"));

        } catch (Exception e) {
            log.error("Update broker config id={} failed", id, e);
            JsonUtil.writeJson(resp, 500, JsonUtil.error("Update failed"));
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            JsonUtil.writeJson(resp, 400, JsonUtil.error("ID required"));
            return;
        }
        try {
            int id = Integer.parseInt(path.substring(1));
            dao.delete(id);
            JsonUtil.writeJson(resp, 200, JsonUtil.success("Deleted"));
        } catch (NumberFormatException e) {
            JsonUtil.writeJson(resp, 400, JsonUtil.error("Invalid ID"));
        }
    }

    private Instant getMidnightToday() {
        return LocalTime.MIDNIGHT.atDate(
                java.time.LocalDate.now().plusDays(1))
                .atZone(ZoneId.systemDefault()).toInstant();
    }

    private String requireString(JsonObject o, String key) {
        if (!o.has(key) || o.get(key).isJsonNull() || o.get(key).getAsString().isBlank())
            throw new IllegalArgumentException(key + " is required");
        return o.get(key).getAsString();
    }

    private String getString(JsonObject o, String key) {
        return (o.has(key) && !o.get(key).isJsonNull()) ? o.get(key).getAsString() : null;
    }
}
