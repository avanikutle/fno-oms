package com.fnooms.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrderDetailsDAO {
    private static final Logger log = LoggerFactory.getLogger(OrderDetailsDAO.class);

    public void insertEntry(String symbol, String orderId, String transactionType, double price, String brokerType) {
        String sql = "INSERT INTO order_details (business_date, order_source, transaction_type, symbol, order_id, price, order_time, updated_by, updated_at, broker_type) " +
                     "VALUES (CURRENT_DATE, 'ALGO', ?, ?, ?, ?, NOW(), ?, NOW(), ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, transactionType);
            pstmt.setString(2, symbol);
            pstmt.setString(3, orderId);
            pstmt.setDouble(4, price);
            pstmt.setString(5, com.fnooms.util.AppConfig.getInstance().getAppUpdater());
            pstmt.setString(6, brokerType);
            pstmt.executeUpdate();
            log.info("Inserted {} order for {} into order_details (orderId={}, broker={})", transactionType, symbol, orderId, brokerType);
        } catch (SQLException e) {
            log.error("Failed to insert into order_details for symbol {}: {}", symbol, e.getMessage());
        }
    }

    public void updateExit(String symbol, String entryOrderId, String exitOrderId, double exitPrice, String brokerType) {
        insertEntry(symbol, exitOrderId, "SELL", exitPrice, brokerType);
    }
}
