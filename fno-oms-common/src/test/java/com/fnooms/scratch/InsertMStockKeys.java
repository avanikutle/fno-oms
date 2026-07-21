package com.fnooms.scratch;

import com.fnooms.dao.DatabaseManager;
import java.sql.Connection;
import java.sql.Statement;

public class InsertMStockKeys {
    public static void main(String[] args) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {

            System.out.println("Inserting missing mStock keys...");
            String[] insertSqls = {
                "INSERT INTO algo_key_value (key_name, key_value, updated_by) VALUES ('mstock.userid', 'MSTOCK_USER_ID', 'SYSTEM') ON CONFLICT (key_name) DO NOTHING;",
                "INSERT INTO algo_key_value (key_name, key_value, updated_by) VALUES ('mstock.pdcred', 'MSTOCK_PDCRED', 'SYSTEM') ON CONFLICT (key_name) DO NOTHING;",
                "INSERT INTO algo_key_value (key_name, key_value, updated_by) VALUES ('mstock.api_key', 'MSTOCK_API_KEY', 'SYSTEM') ON CONFLICT (key_name) DO NOTHING;",
                "INSERT INTO algo_key_value (key_name, key_value, updated_by) VALUES ('mstock.refresh_token', 'MSTOCK_REFRESH_TOKEN', 'SYSTEM') ON CONFLICT (key_name) DO NOTHING;",
                "INSERT INTO algo_key_value (key_name, key_value, updated_by) VALUES ('mock.mstock.ws.url', 'ws://localhost:9090', 'SYSTEM') ON CONFLICT (key_name) DO NOTHING;",
                "INSERT INTO algo_key_value (key_name, key_value, updated_by) VALUES ('mstock.scrip.master.date', '1970-01-01', 'SYSTEM') ON CONFLICT (key_name) DO NOTHING;",
                "INSERT INTO algo_key_value (key_name, key_value, updated_by) VALUES ('mock.mstock.ws.url.enabled', 'true', 'SYSTEM') ON CONFLICT (key_name) DO NOTHING;",
                "INSERT INTO algo_key_value (key_name, key_value, updated_by) VALUES ('mstock.jwt_token', 'MSTOCK_JWT_TOKEN', 'SYSTEM') ON CONFLICT (key_name) DO NOTHING;"
            };

            for (String sql : insertSqls) {
                try {
                    stmt.execute(sql);
                    System.out.println("Executed: " + sql);
                } catch (Exception e) {
                    System.out.println("Warning: Could not insert key. " + e.getMessage());
                }
            }
            
            System.out.println("mStock keys populated!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
