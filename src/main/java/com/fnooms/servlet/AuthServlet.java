package com.fnooms.servlet;

import com.fnooms.dao.AlgoKeyValueDAO;
import com.fnooms.util.JsonUtil;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Handles access token management for brokers.
 *
 * POST /api/auth/token   → save a new access token for the active broker
 *                          (user pastes token from mStock portal)
 */
public class AuthServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(AuthServlet.class);
    private final AlgoKeyValueDAO dao = new AlgoKeyValueDAO();

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

                dao.setValue("mstock.jwt_token", token, "SYSTEM");

                log.info("Access token updated for mStock in algo_key_value");

                JsonObject result = new JsonObject();
                result.addProperty("message",    "Token saved successfully");
                result.addProperty("broker",     "MSTOCK");
                JsonUtil.writeJson(resp, 200, JsonUtil.success(result));

            } catch (IllegalArgumentException e) {
                JsonUtil.writeJson(resp, 400, JsonUtil.error(e.getMessage()));
            } catch (Exception e) {
                log.error("Token save failed", e);
                JsonUtil.writeJson(resp, 500, JsonUtil.error("Failed to save token"));
            }
        } else if ("/login".equals(path)) {
            try {
                String body = new String(req.getInputStream().readAllBytes());
                JsonObject json = JsonUtil.parseObject(body);
                String broker = json.has("broker") ? json.get("broker").getAsString() : "MSTOCK";
                String totp = json.has("totp") ? json.get("totp").getAsString() : "";

                com.fnooms.algo.login.BrokerLoginManager loginManager = new com.fnooms.algo.login.BrokerLoginManager();
                boolean success = loginManager.performWebLogin(broker, totp);

                if (success) {
                    JsonObject result = new JsonObject();
                    result.addProperty("message", "Login successful for " + broker);
                    result.addProperty("broker", broker);
                    JsonUtil.writeJson(resp, 200, JsonUtil.success(result));
                } else {
                    JsonUtil.writeJson(resp, 401, JsonUtil.error("Login failed for " + broker));
                }
            } catch (Exception e) {
                log.error("Login endpoint error", e);
                JsonUtil.writeJson(resp, 500, JsonUtil.error(e.getMessage()));
            }
        } else {
            JsonUtil.writeJson(resp, 404, JsonUtil.error("Unknown auth endpoint"));
        }
    }
}
