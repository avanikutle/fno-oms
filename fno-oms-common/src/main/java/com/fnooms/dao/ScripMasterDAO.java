package com.fnooms.dao;

import com.google.gson.JsonObject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScripMasterDAO {
    private static final Logger log = LoggerFactory.getLogger(ScripMasterDAO.class);

    public void batchInsert(List<JsonObject> instruments) {
        String truncateSql = "TRUNCATE TABLE angelone_scrip_master";
        String insertSql = """
                INSERT INTO angelone_scrip_master (token, symbol, name, expiry, strike, lot_size, instrument_type, exch_seg, tick_size)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (token) DO NOTHING
                """;

        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            c.setAutoCommit(false);
            
            // Truncate the table first
            try (Statement s = c.createStatement()) {
                s.executeUpdate(truncateSql);
                log.info("Truncated angelone_scrip_master table");
            }
            
            try (PreparedStatement ps = c.prepareStatement(insertSql)) {
                int count = 0;
                for (JsonObject obj : instruments) {
                    try {
                        ps.setString(1, obj.get("token").getAsString());
                        ps.setString(2, obj.get("symbol").getAsString());
                        ps.setString(3, obj.get("name").getAsString());
                        ps.setString(4, obj.get("expiry").getAsString());
                        
                        String strikeStr = obj.get("strike").getAsString();
                        ps.setBigDecimal(5, strikeStr.isEmpty() ? null : new java.math.BigDecimal(strikeStr));
                        
                        String lotSizeStr = obj.get("lotsize").getAsString();
                        ps.setInt(6, lotSizeStr.isEmpty() ? 0 : Integer.parseInt(lotSizeStr));
                        
                        ps.setString(7, obj.get("instrumenttype").getAsString());
                        ps.setString(8, obj.get("exch_seg").getAsString());
                        
                        String tickSizeStr = obj.get("tick_size").getAsString();
                        ps.setBigDecimal(9, tickSizeStr.isEmpty() ? null : new java.math.BigDecimal(tickSizeStr));

                        ps.addBatch();
                        count++;

                        if (count % 5000 == 0) {
                            ps.executeBatch();
                        }
                    } catch (Exception e) {
                        log.warn("Skipping malformed scrip obj: {}", obj, e);
                    }
                }
                ps.executeBatch(); // flush remaining
                c.commit();
                log.info("Successfully batch inserted {} instruments into angelone_scrip_master", count);
            } catch (SQLException e) {
                c.rollback();
                log.error("Failed batch insert, rolled back", e);
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            log.error("Database connection error during batch insert", e);
        }
    }

    public String getTokenBySymbol(String symbol) {
        String sql = "SELECT token FROM angelone_scrip_master WHERE symbol = ?";
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, symbol);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("token");
            }
        } catch (SQLException e) {
            log.error("Failed to query token for symbol: {}", symbol, e);
        }
        return null;
    }
}
