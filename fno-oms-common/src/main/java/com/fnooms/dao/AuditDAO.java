package com.fnooms.dao;

import com.fnooms.model.AuditLog;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuditDAO {

    private static final Logger log = LoggerFactory.getLogger(AuditDAO.class);

    public void insert(AuditLog entry) {
        String sql = """
                INSERT INTO audit_log
                  (action, broker_type, endpoint, request, response,
                   status_code, latency_ms, error, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())
                """;
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, entry.getAction());
            ps.setString(2, entry.getBrokerType());
            ps.setString(3, entry.getEndpoint());
            ps.setString(4, entry.getRequest());
            ps.setString(5, entry.getResponse());
            ps.setInt(6,    entry.getStatusCode());
            ps.setLong(7,   entry.getLatencyMs());
            ps.setString(8, entry.getError());
            ps.executeUpdate();
        } catch (SQLException e) {
            log.warn("AuditDAO insert failed (non-critical): {}", e.getMessage());
        }
    }

    public List<AuditLog> findRecent(int limit) {
        String sql = "SELECT * FROM audit_log ORDER BY created_at DESC LIMIT ?";
        List<AuditLog> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("AuditDAO findRecent failed", e);
        }
        return list;
    }

    private AuditLog mapRow(ResultSet rs) throws SQLException {
        AuditLog a = new AuditLog();
        a.setId(rs.getLong("id"));
        a.setAction(rs.getString("action"));
        a.setBrokerType(rs.getString("broker_type"));
        a.setEndpoint(rs.getString("endpoint"));
        a.setRequest(rs.getString("request"));
        a.setResponse(rs.getString("response"));
        a.setStatusCode(rs.getInt("status_code"));
        a.setLatencyMs(rs.getLong("latency_ms"));
        a.setError(rs.getString("error"));
        a.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        return a;
    }
}
