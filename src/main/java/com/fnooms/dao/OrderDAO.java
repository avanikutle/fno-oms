package com.fnooms.dao;

import com.fnooms.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderDAO {

    private static final Logger log = LoggerFactory.getLogger(OrderDAO.class);

    public void insert(Order o) {
        String sql = """
                INSERT INTO orders
                  (business_date, order_source, broker_order_id, broker_type, symbol, exchange,
                   transaction_type, order_type, product, quantity, price,
                   trigger_price, validity, status, status_message,
                   filled_quantity, average_price, exchange_order_id,
                   exchange_timestamp, placed_at, updated_at)
                VALUES (CURRENT_DATE,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW())
                """;
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1,  o.getOrderSource());
            ps.setString(2,  o.getBrokerOrderId());
            ps.setString(3,  o.getBrokerType());
            ps.setString(4,  o.getSymbol());
            ps.setString(5,  o.getExchange());
            ps.setString(6,  o.getTransactionType());
            ps.setString(7,  o.getOrderType());
            ps.setString(8,  o.getProduct());
            ps.setInt(9,     o.getQuantity());
            ps.setBigDecimal(10, o.getPrice());
            ps.setBigDecimal(11, o.getTriggerPrice());
            ps.setString(12, o.getValidity());
            ps.setString(13, o.getStatus());
            ps.setString(14, o.getStatusMessage());
            ps.setInt(15,    o.getFilledQuantity());
            ps.setBigDecimal(16, o.getAveragePrice());
            ps.setString(17, o.getExchangeOrderId());
            ps.setTimestamp(18, o.getExchangeTimestamp() != null
                    ? Timestamp.from(o.getExchangeTimestamp()) : null);
            ps.setTimestamp(19, o.getPlacedAt() != null
                    ? Timestamp.from(o.getPlacedAt()) : null);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Insert order failed for symbol={}", o.getSymbol(), e);
        }
    }

    public void updateStatus(Order o) {
        String sql = """
                UPDATE orders SET
                  status = ?, status_message = ?, filled_quantity = ?,
                  average_price = ?, exchange_order_id = ?,
                  exchange_timestamp = ?, updated_at = NOW()
                WHERE broker_order_id = ?
                """;
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, o.getStatus());
            ps.setString(2, o.getStatusMessage());
            ps.setInt(3,    o.getFilledQuantity());
            ps.setBigDecimal(4, o.getAveragePrice());
            ps.setString(5, o.getExchangeOrderId());
            ps.setTimestamp(6, o.getExchangeTimestamp() != null
                    ? Timestamp.from(o.getExchangeTimestamp()) : null);
            ps.setString(7, o.getBrokerOrderId());
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Update order status failed for id={}", o.getBrokerOrderId(), e);
        }
    }

    public void updateOrderStatus(String brokerOrderId, String status, String message) {
        String sql = "UPDATE orders SET status = ?, status_message = ?, updated_at = NOW() WHERE broker_order_id = ?";
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, message);
            ps.setString(3, brokerOrderId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Update order status failed for id={}", brokerOrderId, e);
        }
    }

    public List<Order> findTodaysOrders() {
        String sql = """
                SELECT * FROM orders
                WHERE placed_at >= CURRENT_DATE
                ORDER BY placed_at DESC
                """;
        return queryList(sql);
    }

    public List<Order> findBySymbol(String symbol) {
        String sql = "SELECT * FROM orders WHERE symbol = ? ORDER BY placed_at DESC LIMIT 100";
        List<Order> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, symbol);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("findBySymbol failed for symbol={}", symbol, e);
        }
        return list;
    }

    public boolean hasOpenOrderForSymbol(String symbol) {
        String sql = "SELECT COUNT(*) FROM orders WHERE symbol = ? AND status IN ('OPEN', 'PENDING', 'O-Pending', 'TRIGGER_PENDING')";
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, symbol);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            log.error("hasOpenOrderForSymbol failed for symbol={}", symbol, e);
        }
        return false;
    }

    private List<Order> queryList(String sql) {
        List<Order> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            log.error("OrderDAO query failed", e);
        }
        return list;
    }

    private Order mapRow(ResultSet rs) throws SQLException {
        Order o = new Order();
        o.setId(rs.getLong("id"));
        o.setBrokerOrderId(rs.getString("broker_order_id"));
        o.setBrokerType(rs.getString("broker_type"));
        o.setSymbol(rs.getString("symbol"));
        o.setExchange(rs.getString("exchange"));
        o.setTransactionType(rs.getString("transaction_type"));
        o.setOrderType(rs.getString("order_type"));
        o.setProduct(rs.getString("product"));
        o.setQuantity(rs.getInt("quantity"));
        o.setPrice(rs.getBigDecimal("price"));
        o.setTriggerPrice(rs.getBigDecimal("trigger_price"));
        o.setValidity(rs.getString("validity"));
        o.setStatus(rs.getString("status"));
        o.setStatusMessage(rs.getString("status_message"));
        o.setFilledQuantity(rs.getInt("filled_quantity"));
        o.setAveragePrice(rs.getBigDecimal("average_price"));
        o.setExchangeOrderId(rs.getString("exchange_order_id"));
        Timestamp et = rs.getTimestamp("exchange_timestamp");
        if (et != null) o.setExchangeTimestamp(et.toInstant());
        o.setPlacedAt(rs.getTimestamp("placed_at").toInstant());
        o.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
        return o;
    }
}
