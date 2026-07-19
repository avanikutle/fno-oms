package com.fnooms.dao;

import com.fnooms.async.event.TickEvent;
import java.sql.*;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TickDAO {

    private static final Logger log = LoggerFactory.getLogger(TickDAO.class);

    /**
     * Bulk insert a batch of ticks using a single prepared statement.
     * Extremely efficient for TimescaleDB — thousands of rows/second.
     */
    public void batchInsert(List<TickEvent> ticks) {
        if (ticks == null || ticks.isEmpty()) return;

        String sql = """
                INSERT INTO quote_ticks
                  (time, symbol, exchange, ltp, open, high, low, close,
                   bid, ask, volume, oi, change, change_pct, broker_type)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            c.setAutoCommit(false);
            for (TickEvent t : ticks) {
                ps.setTimestamp(1,  Timestamp.from(t.getTime()));
                ps.setString(2,     t.getSymbol());
                ps.setString(3,     t.getExchange());
                ps.setBigDecimal(4, t.getLtp());
                ps.setBigDecimal(5, t.getOpen());
                ps.setBigDecimal(6, t.getHigh());
                ps.setBigDecimal(7, t.getLow());
                ps.setBigDecimal(8, t.getClose());
                ps.setBigDecimal(9, t.getBid());
                ps.setBigDecimal(10, t.getAsk());
                ps.setLong(11,      t.getVolume());
                ps.setLong(12,      t.getOi());
                ps.setBigDecimal(13, t.getChange());
                ps.setBigDecimal(14, t.getChangePct());
                ps.setString(15,    t.getBrokerType());
                ps.addBatch();
            }
            ps.executeBatch();
            c.commit();
        } catch (SQLException e) {
            log.error("TickDAO batchInsert failed for {} ticks", ticks.size(), e);
        }
    }
}
