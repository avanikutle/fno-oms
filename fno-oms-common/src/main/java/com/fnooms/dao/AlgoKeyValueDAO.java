package com.fnooms.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AlgoKeyValueDAO {
    private static final Logger log = LoggerFactory.getLogger(AlgoKeyValueDAO.class);

    public String getValue(String key) {
        String sql = "SELECT key_value FROM algo_key_value WHERE key_name = ?";
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (SQLException e) {
            log.error("getValue failed for key={}", key, e);
        }
        return null;
    }

    public void setValue(String key, String value, String updatedBy) {
        String sql = """
                INSERT INTO algo_key_value (key_name, key_value, updated_by) 
                VALUES (?, ?, ?) 
                ON CONFLICT (key_name) 
                DO UPDATE SET key_value = EXCLUDED.key_value, updated_by = EXCLUDED.updated_by, updated_at = NOW()
                """;
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.setString(3, updatedBy != null ? updatedBy : com.fnooms.util.AppConfig.getInstance().getAppUpdater());
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("setValue failed for key={}", key, e);
        }
    }
    
    public void setValue(String key, String value) {
        setValue(key, value, com.fnooms.util.AppConfig.getInstance().getAppUpdater());
    }
}
