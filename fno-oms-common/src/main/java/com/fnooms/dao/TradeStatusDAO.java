package com.fnooms.dao;

import com.fnooms.algo.TradeState;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TradeStatusDAO {
    private static final Logger log = LoggerFactory.getLogger(TradeStatusDAO.class);

    /**
     * Loads the latest state for the given strategyId for today.
     * If no active record exists, returns a clean new TradeState.
     */
    public TradeState loadLatestState(Long strategyId) {
        String sql = "SELECT id, entered, exited, entry_price, current_target, current_stop_loss, entry_order_id, exit_order_id " +
                     "FROM trade_status WHERE biz_date = CURRENT_DATE AND strategy_id = ? ORDER BY id DESC LIMIT 1";

        TradeState state = new TradeState();

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, strategyId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    boolean exited = rs.getBoolean("exited");
                    
                    // Load ongoing or finished trade
                    state.setDbId(rs.getLong("id"));
                    state.setEntered(rs.getBoolean("entered"));
                    state.setExited(exited);
                    state.setEntryPrice(rs.getDouble("entry_price"));
                    state.setCurrentTarget(rs.getDouble("current_target"));
                    state.setCurrentStopLoss(rs.getDouble("current_stop_loss"));
                    state.setEntryOrderId(rs.getString("entry_order_id"));
                    state.setExitOrderId(rs.getString("exit_order_id"));
                }
            }
        } catch (Exception e) {
            log.error("Error loading trade status for strategyId {}: {}", strategyId, e.getMessage());
        }
        return state;
    }

    /**
     * Inserts a brand new trade status row (used on initial entry).
     */
    public void saveNewState(Long strategyId, String symbol, String exchangeToken, TradeState state, String comments, String brokerType) {
        String sql = "INSERT INTO trade_status (strategy_id, symbol, exchange_token, entered, exited, entry_price, current_target, current_stop_loss, entry_order_id, exit_order_id, comments, broker_type) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, strategyId);
            pstmt.setString(2, symbol);
            pstmt.setString(3, exchangeToken);
            pstmt.setBoolean(4, state.isEntered());
            pstmt.setBoolean(5, state.isExited());
            pstmt.setDouble(6, state.getEntryPrice());
            pstmt.setDouble(7, state.getCurrentTarget());
            pstmt.setDouble(8, state.getCurrentStopLoss());
            pstmt.setString(9, state.getEntryOrderId());
            pstmt.setString(10, state.getExitOrderId());
            pstmt.setString(11, comments);
            pstmt.setString(12, brokerType);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    state.setDbId(rs.getLong("id"));
                }
            }
        } catch (Exception e) {
            log.error("Error saving new trade status for strategyId {}: {}", strategyId, e.getMessage());
        }
    }

    /**
     * Updates an existing active trade status row.
     */
    public void updateState(String symbol, TradeState state, String comments) {
        if (state.getDbId() == null) {
            log.warn("Cannot update trade status for {} without a dbId. Are you updating a fresh state?", symbol);
            return;
        }

        String sql = "UPDATE trade_status SET entered = ?, exited = ?, entry_price = ?, current_target = ?, current_stop_loss = ?, " +
                     "entry_order_id = ?, exit_order_id = ?, comments = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setBoolean(1, state.isEntered());
            pstmt.setBoolean(2, state.isExited());
            pstmt.setDouble(3, state.getEntryPrice());
            pstmt.setDouble(4, state.getCurrentTarget());
            pstmt.setDouble(5, state.getCurrentStopLoss());
            pstmt.setString(6, state.getEntryOrderId());
            pstmt.setString(7, state.getExitOrderId());
            pstmt.setString(8, comments);
            pstmt.setLong(9, state.getDbId());

            pstmt.executeUpdate();
        } catch (Exception e) {
            log.error("Error updating trade status for {}: {}", symbol, e.getMessage());
        }
    }
}
