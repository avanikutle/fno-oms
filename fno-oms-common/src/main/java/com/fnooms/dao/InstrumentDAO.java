package com.fnooms.dao;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstrumentDAO {
    private static final Logger log = LoggerFactory.getLogger(InstrumentDAO.class);
    private static final InstrumentDAO INSTANCE = new InstrumentDAO();
    
    // In-memory cache of options for selected base symbols
    private final List<JsonObject> cachedOptions = new CopyOnWriteArrayList<>();

    private InstrumentDAO() {}

    public static InstrumentDAO getInstance() {
        return INSTANCE;
    }

    /**
     * Loads options for the given base symbols (e.g. NIFTY, BANKNIFTY) into memory.
     */
    public void loadCache(List<String> baseSymbols) {
        cachedOptions.clear();
        if (baseSymbols == null || baseSymbols.isEmpty()) {
            return;
        }

        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < baseSymbols.size(); i++) {
            placeholders.append("?");
            if (i < baseSymbols.size() - 1) placeholders.append(",");
        }

        String sql = "SELECT instrument_token, instrument_name, tradingsymbol, expiry_date, strike_price, option_type, exchange_segment " +
                     "FROM mstock_scrip_master " +
                     "WHERE instrument_name IN (" + placeholders.toString() + ") " +
                     "AND option_type IN ('OPTIDX', 'OPTSTK') " +
                     "ORDER BY expiry_date, strike_price";

        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            
            for (int i = 0; i < baseSymbols.size(); i++) {
                ps.setString(i + 1, baseSymbols.get(i).trim().toUpperCase());
            }

            try (ResultSet rs = ps.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("token", rs.getString("instrument_token"));
                    obj.addProperty("name", rs.getString("instrument_name")); // e.g. NIFTY
                    String sym = rs.getString("tradingsymbol");
                    obj.addProperty("symbol", sym); // e.g. NIFTY24JUL24000CE
                    obj.addProperty("expiry", rs.getString("expiry_date"));
                    obj.addProperty("strike", rs.getDouble("strike_price"));
                    
                    String type = sym.endsWith("CE") ? "CE" : (sym.endsWith("PE") ? "PE" : "XX");
                    obj.addProperty("type", type);
                    
                    obj.addProperty("exchange", rs.getString("exchange_segment"));
                    cachedOptions.add(obj);
                    count++;
                }
                log.info("Loaded {} options into Instrument Cache for symbols: {}", count, baseSymbols);
            }
        } catch (SQLException e) {
            log.error("Failed to load instrument cache", e);
        }
    }

    /**
     * Search the in-memory cache.
     */
    public JsonArray search(String query, int limit) {
        JsonArray results = new JsonArray();
        if (query == null || query.trim().isEmpty()) {
            for (JsonObject opt : cachedOptions) {
                results.add(opt);
                if (results.size() >= limit) break;
            }
            return results;
        }
        
        String q = query.trim().toUpperCase();
        int count = 0;
        
        for (JsonObject opt : cachedOptions) {
            String symbol = opt.get("symbol").getAsString();
            if (symbol.contains(q)) {
                results.add(opt);
                count++;
                if (count >= limit) break;
            }
        }
        return results;
    }

    public List<String> getAllTokens() {
        List<String> tokens = new java.util.ArrayList<>();
        for (JsonObject opt : cachedOptions) {
            tokens.add(opt.get("token").getAsString());
        }
        return tokens;
    }

    public List<String> getAllSymbols() {
        List<String> symbols = new java.util.ArrayList<>();
        for (JsonObject opt : cachedOptions) {
            symbols.add(opt.get("symbol").getAsString());
        }
        return symbols;
    }

    public List<JsonObject> getAllCachedOptions() {
        return new java.util.ArrayList<>(cachedOptions);
    }
}
