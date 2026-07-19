package com.fnooms.servlet;

import com.fnooms.dao.InstrumentDAO;
import com.fnooms.util.JsonUtil;
import com.google.gson.JsonArray;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * REST endpoint for searching cached instruments.
 * GET /api/instruments/search?q=NIFTY24JUL
 */
public class InstrumentServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String query = req.getParameter("q");
        if (query == null) {
            query = "";
        }
        
        try {
            JsonArray results = InstrumentDAO.getInstance().search(query, 20); // max 20 results
            JsonUtil.writeJson(resp, 200, JsonUtil.success(results));
        } catch (Exception e) {
            JsonUtil.writeJson(resp, 500, JsonUtil.error("Failed to search instruments: " + e.getMessage()));
        }
    }
}
