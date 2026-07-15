package com.fnooms.util;

import com.fnooms.dao.DatabaseManager;
import java.sql.Connection;
import java.sql.Statement;

public class MigrateDb {
    public static void main(String[] args) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            
            String sql = "CREATE TABLE IF NOT EXISTS mstock_scrip_master (" +
                "instrument_token VARCHAR(50) PRIMARY KEY," +
                "exchange_token VARCHAR(50)," +
                "tradingsymbol VARCHAR(100)," +
                "name VARCHAR(200)," +
                "last_price DOUBLE PRECISION," +
                "expiry VARCHAR(50)," +
                "strike DOUBLE PRECISION," +
                "tick_size DOUBLE PRECISION," +
                "lot_size INTEGER," +
                "instrument_type VARCHAR(50)," +
                "segment VARCHAR(50)," +
                "exchange VARCHAR(50)" +
            ");" +
            "CREATE TABLE IF NOT EXISTS strategies (" +
                "scrip_name VARCHAR(100) PRIMARY KEY," +
                "exchange_id VARCHAR(50)," +
                "name VARCHAR(100)," +
                "entry_price DOUBLE PRECISION," +
                "stop_loss DOUBLE PRECISION," +
                "target_price DOUBLE PRECISION," +
                "quantity INTEGER," +
                "transaction_type VARCHAR(20)," +
                "entry_condition VARCHAR(50)," +
                "product VARCHAR(20)," +
                "trailing_sl_points DOUBLE PRECISION" +
            ");" + 
            "INSERT INTO strategies (scrip_name, exchange_id, name, entry_price, stop_loss, target_price, quantity, transaction_type, entry_condition, product, trailing_sl_points) " +
            "VALUES ('NIFTY21JUL2624200CE', 'NFO', 'NIFTY21JUL2624200CE', 88.0, 80.0, 101.0, 1, 'BUY', 'GREATER_THAN_EQUAL', 'NRML', 2.0) ON CONFLICT(scrip_name) DO NOTHING;";
            
            stmt.executeUpdate(sql);
            System.out.println("Tables created and strategy populated successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
