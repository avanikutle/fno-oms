package com.fnooms.scratch;

import com.fnooms.dao.DatabaseManager;
import java.sql.Connection;
import java.sql.Statement;

public class MigrateStateDb {
    public static void main(String[] args) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {

            System.out.println("Dropping unique constraint on strategies.scrip_name...");
            try {
                stmt.execute("ALTER TABLE strategies DROP CONSTRAINT IF EXISTS strategies_scrip_name_key");
                System.out.println("Dropped strategies_scrip_name_key successfully.");
            } catch (Exception e) {
                System.out.println("Note: strategies_scrip_name_key drop error: " + e.getMessage());
            }

            System.out.println("Adding strategy_id to trade_status...");
            try {
                stmt.execute("ALTER TABLE trade_status ADD COLUMN IF NOT EXISTS strategy_id INT");
                System.out.println("Added strategy_id successfully.");
            } catch (Exception e) {
                System.out.println("Note: strategy_id add error: " + e.getMessage());
            }

            System.out.println("Migration complete!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
