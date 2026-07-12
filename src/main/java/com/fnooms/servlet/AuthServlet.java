package com.fnooms.servlet;

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
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Handles access token management for brokers.
 *
 * POST /api/auth/token   → save a new access token for the active broker
 *                          (user pastes token from mStock portal)
 */
public class AuthServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(AuthServlet.class);
    private final BrokerConfigDAO dao = new BrokerConfigDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();

        if ("/token".equals(path)) {
            try {
                String body = new String(req.getInputStream().readAllBytes());
                JsonObject json = JsonUtil.parseObject(body);

                int configId = json.has("configId") ? json.get("configId").getAsInt() : -1;
                String token = json.has("accessToken")
                        ? json.get("accessToken").getAsString() : null;

                if (token == null || token.isBlank())
                    throw new IllegalArgumentException("accessToken is required");

                BrokerConfig cfg = configId > 0
                        ? dao.findById(configId) : dao.getActive();

                if (cfg == null) {
                    JsonUtil.writeJson(resp, 404, JsonUtil.error("Broker config not found"));
                    return;
                }

                cfg.setAccessToken(token);
                // Token valid till midnight
                cfg.setTokenExpiry(LocalDateTime.now()
                        .toLocalDate().plusDays(1)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant());
                dao.update(cfg);

                log.info("Access token updated for broker={} configId={}", cfg.getBrokerType(), cfg.getId());

                JsonObject result = new JsonObject();
                result.addProperty("message",    "Token saved successfully");
                result.addProperty("broker",     cfg.getDisplayName());
                result.addProperty("expiresAt",  cfg.getTokenExpiry().toString());
                JsonUtil.writeJson(resp, 200, JsonUtil.success(result));

            } catch (IllegalArgumentException e) {
                JsonUtil.writeJson(resp, 400, JsonUtil.error(e.getMessage()));
            } catch (Exception e) {
                log.error("Token save failed", e);
                JsonUtil.writeJson(resp, 500, JsonUtil.error("Failed to save token"));
            }
        } else {
            JsonUtil.writeJson(resp, 404, JsonUtil.error("Unknown auth endpoint"));
        }
    }
}
