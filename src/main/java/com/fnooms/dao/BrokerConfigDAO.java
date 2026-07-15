package com.fnooms.dao;

import com.fnooms.model.BrokerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class BrokerConfigDAO {

    private static final Logger log = LoggerFactory.getLogger(BrokerConfigDAO.class);

    public List<BrokerConfig> findAll() {
        List<BrokerConfig> list = new ArrayList<>();
        String sql = "SELECT * FROM broker_config ORDER BY id";
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            log.error("findAll brokerConfig failed", e);
        }
        return list;
    }

    public BrokerConfig getActive() {
        String sql = "SELECT * FROM broker_config WHERE is_active = TRUE LIMIT 1";
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            log.error("getActive brokerConfig failed", e);
        }
        return null;
    }

    public BrokerConfig findByType(String brokerType) {
        String sql = "SELECT * FROM broker_config WHERE broker_type = ? LIMIT 1";
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, brokerType);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            log.error("findByType brokerConfig type={} failed", brokerType, e);
        }
        return null;
    }


    public BrokerConfig findById(int id) {
        String sql = "SELECT * FROM broker_config WHERE id = ?";
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            log.error("findById brokerConfig id={} failed", id, e);
        }
        return null;
    }

    public int insert(BrokerConfig cfg) {
        String sql = """
                INSERT INTO broker_config
                  (broker_type, display_name, api_key, private_key, access_token,
                   token_expiry, client_id, is_active)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """;
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, cfg.getBrokerType());
            ps.setString(2, cfg.getDisplayName());
            ps.setString(3, cfg.getApiKey());
            ps.setString(4, cfg.getPrivateKey());
            ps.setString(5, cfg.getAccessToken());
            ps.setTimestamp(6, cfg.getTokenExpiry() != null
                    ? Timestamp.from(cfg.getTokenExpiry()) : null);
            ps.setString(7, cfg.getClientId());
            ps.setBoolean(8, cfg.isActive());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            log.error("insert brokerConfig failed", e);
        }
        return -1;
    }

    public void update(BrokerConfig cfg) {
        String sql = """
                UPDATE broker_config SET
                  display_name = ?, api_key = ?, private_key = ?,
                  access_token = ?, token_expiry = ?, client_id = ?,
                  is_active = ?, updated_at = NOW()
                WHERE id = ?
                """;
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, cfg.getDisplayName());
            ps.setString(2, cfg.getApiKey());
            ps.setString(3, cfg.getPrivateKey());
            ps.setString(4, cfg.getAccessToken());
            ps.setTimestamp(5, cfg.getTokenExpiry() != null
                    ? Timestamp.from(cfg.getTokenExpiry()) : null);
            ps.setString(6, cfg.getClientId());
            ps.setBoolean(7, cfg.isActive());
            ps.setInt(8, cfg.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("update brokerConfig id={} failed", cfg.getId(), e);
        }
    }

    /** Set a broker as active, deactivating all others atomically. */
    public void setActive(int id) {
        String deactivate = "UPDATE broker_config SET is_active = FALSE, updated_at = NOW()";
        String activate   = "UPDATE broker_config SET is_active = TRUE,  updated_at = NOW() WHERE id = ?";
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            c.setAutoCommit(false);
            try (Statement s = c.createStatement()) {
                s.executeUpdate(deactivate);
            }
            try (PreparedStatement ps = c.prepareStatement(activate)) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            c.commit();
        } catch (SQLException e) {
            log.error("setActive brokerConfig id={} failed", id, e);
        }
    }

    public void delete(int id) {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "DELETE FROM broker_config WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("delete brokerConfig id={} failed", id, e);
        }
    }

    private BrokerConfig mapRow(ResultSet rs) throws SQLException {
        BrokerConfig cfg = new BrokerConfig();
        cfg.setId(rs.getInt("id"));
        cfg.setBrokerType(rs.getString("broker_type"));
        cfg.setDisplayName(rs.getString("display_name"));
        cfg.setApiKey(rs.getString("api_key"));
        cfg.setPrivateKey(rs.getString("private_key"));
        cfg.setAccessToken(rs.getString("access_token"));
        Timestamp exp = rs.getTimestamp("token_expiry");
        if (exp != null) cfg.setTokenExpiry(exp.toInstant());
        cfg.setClientId(rs.getString("client_id"));
        cfg.setActive(rs.getBoolean("is_active"));
        cfg.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        cfg.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
        return cfg;
    }
}
