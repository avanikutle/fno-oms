package com.fnooms.algo.login;

import com.fnooms.dao.DatabaseManager;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class DbCheck {
    public static void main(String[] args) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
             
            System.out.println("Checking algo_key_value table for groww keys...");
            ResultSet rs = stmt.executeQuery("SELECT key_name, key_value FROM algo_key_value WHERE key_name LIKE 'groww.%'");
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.println("Found key: " + rs.getString("key_name") + " = [REDACTED]");
            }
            if (!found) {
                System.out.println("No groww keys found. Inserting placeholder groww.access_token...");
                stmt.executeUpdate("INSERT INTO algo_key_value (key_name, key_value) VALUES ('groww.access_token', 'YOUR_GROWW_TOKEN_HERE') ON CONFLICT DO NOTHING");
                System.out.println("Placeholder inserted.");
            }
            
            System.out.println("Checking if groww_scrip_master exists...");
            ResultSet rsTable = stmt.executeQuery("SELECT to_regclass('public.groww_scrip_master')");
            if (rsTable.next() && rsTable.getString(1) != null) {
                System.out.println("Table groww_scrip_master exists.");
            } else {
                System.out.println("Table groww_scrip_master does not exist yet. It will be created when you run the Scrip Master fetcher.");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
