package com.fnooms.servlet;

import com.fnooms.dao.AlgoKeyValueDAO;
import com.fnooms.dao.InstrumentDAO;
import com.fnooms.util.JsonUtil;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * REST endpoint for system settings.
 */
public class SettingsServlet extends HttpServlet {

    private final AlgoKeyValueDAO kvDao = new AlgoKeyValueDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if ("/watchlist-basesymbols".equals(path)) {
            String val = kvDao.getValue("app.watchlist.basesymbols");
            if (val == null) val = "NIFTY,BANKNIFTY,SENSEX";
            JsonUtil.writeJson(resp, 200, JsonUtil.success(val));
            return;
        }
        JsonUtil.writeJson(resp, 404, JsonUtil.error("Not found"));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if ("/watchlist-basesymbols".equals(path)) {
            JsonObject body;
            try {
                body = com.google.gson.JsonParser.parseReader(req.getReader()).getAsJsonObject();
            } catch (Exception e) {
                JsonUtil.writeJson(resp, 400, JsonUtil.error("Invalid JSON body"));
                return;
            }
            if (body == null || !body.has("symbols")) {
                JsonUtil.writeJson(resp, 400, JsonUtil.error("Missing 'symbols'"));
                return;
            }
            String symbols = body.get("symbols").getAsString();
            kvDao.setValue("app.watchlist.basesymbols", symbols);
            
            // Reload cache in background or immediately
            List<String> baseSymbols = Arrays.asList(symbols.split(","));
            InstrumentDAO.getInstance().loadCache(baseSymbols);
            
            JsonUtil.writeJson(resp, 200, JsonUtil.success("Saved"));
            return;
        }
        JsonUtil.writeJson(resp, 404, JsonUtil.error("Not found"));
    }
}
