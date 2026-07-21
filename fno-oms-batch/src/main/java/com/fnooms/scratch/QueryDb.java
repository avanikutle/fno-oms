package com.fnooms.scratch;

import java.sql.*;

public class QueryDb {
    public static void main(String[] args) throws Exception {
        
        try (java.sql.Connection conn = com.fnooms.dao.DatabaseManager.getInstance().getConnection();
             java.sql.ResultSet rs = conn.getMetaData().getColumns(null, null, "trade_status", null)) {
            System.out.println("trade_status columns:");
            while (rs.next()) {
                System.out.println(rs.getString("COLUMN_NAME") + " - " + rs.getString("TYPE_NAME"));
            }
        }
    }
}
