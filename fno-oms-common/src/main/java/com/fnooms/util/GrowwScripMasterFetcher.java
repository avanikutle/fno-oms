package com.fnooms.util;

import com.fnooms.dao.DatabaseManager;
import java.io.BufferedReader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrowwScripMasterFetcher {
    private static final Logger log = LoggerFactory.getLogger(GrowwScripMasterFetcher.class);
    private static final OkHttpClient client = new OkHttpClient();

    public static void fetchAndStoreScripMaster() {
        String url = BrokerConfig.getProperty("groww.api.instruments", "https://growwapi-assets.groww.in/instruments/instrument.csv");
        log.info("Starting download of Groww Scrip Master from: {}", url);

        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("Failed to download CSV: HTTP code {}", response.code());
                return;
            }

            String csvContent = response.body().string();
            processCsvAndStore(csvContent);

        } catch (Exception e) {
            log.error("Error fetching Groww Scrip Master", e);
        }
    }

    private static void processCsvAndStore(String csvContent) {
        String dropTableSql = "DROP TABLE IF EXISTS groww_scrip_master CASCADE";
        String createTableSql = "CREATE TABLE groww_scrip_master (" +
                "id SERIAL PRIMARY KEY, " +
                "exchange VARCHAR(50), " +
                "exchange_token VARCHAR(50), " +
                "trading_symbol VARCHAR(100), " +
                "groww_symbol VARCHAR(100), " +
                "name VARCHAR(255), " +
                "instrument_type VARCHAR(50), " +
                "segment VARCHAR(50), " +
                "series VARCHAR(50), " +
                "isin VARCHAR(50), " +
                "lot_size INT, " +
                "expiry_date VARCHAR(50), " +
                "strike_price NUMERIC, " +
                "tick_size NUMERIC, " +
                "updated_by varchar(50) DEFAULT 'SYSTEM'::character varying NULL, " +
                "updated_at timestamptz DEFAULT now() NULL" +
                ")";

        String insertSql = "INSERT INTO groww_scrip_master (exchange, exchange_token, trading_symbol, groww_symbol, " +
                "name, instrument_type, segment, series, isin, lot_size, expiry_date, strike_price, tick_size) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {

            // Drop and Create table
            stmt.execute(dropTableSql);
            stmt.execute(createTableSql);

            try (PreparedStatement pstmt = conn.prepareStatement(insertSql);
                 BufferedReader reader = new BufferedReader(new StringReader(csvContent))) {
                 
                String line;
                boolean isFirst = true;
                int count = 0;
                
                while ((line = reader.readLine()) != null) {
                    if (isFirst) {
                        isFirst = false;
                        continue; // Skip header
                    }
                    
                    // Simple split by comma. Note: If fields contain commas enclosed in quotes, a proper CSV parser is needed.
                    String[] fields = line.split(",", -1);
                    if (fields.length < 15) continue; // Ensure we have enough columns

                    pstmt.setString(1, fields[0]); // exchange
                    pstmt.setString(2, fields[1]); // exchange_token
                    pstmt.setString(3, fields[2]); // trading_symbol
                    pstmt.setString(4, fields[3]); // groww_symbol
                    pstmt.setString(5, fields[4]); // name
                    pstmt.setString(6, fields[5]); // instrument_type
                    pstmt.setString(7, fields[6]); // segment
                    pstmt.setString(8, fields[7]); // series
                    pstmt.setString(9, fields[8]); // isin
                    
                    try { pstmt.setInt(10, fields[13].isEmpty() ? 0 : Integer.parseInt(fields[13])); } catch (Exception e) { pstmt.setInt(10, 0); } // lot_size
                    pstmt.setString(11, fields[11]); // expiry_date
                    try { pstmt.setBigDecimal(12, fields[12].isEmpty() ? null : new java.math.BigDecimal(fields[12])); } catch (Exception e) { pstmt.setBigDecimal(12, null); } // strike_price
                    try { pstmt.setBigDecimal(13, fields[14].isEmpty() ? null : new java.math.BigDecimal(fields[14])); } catch (Exception e) { pstmt.setBigDecimal(13, null); } // tick_size

                    pstmt.addBatch();
                    count++;
                    
                    if (count % 5000 == 0) {
                        pstmt.executeBatch();
                        log.info("Inserted {} records...", count);
                    }
                }
                
                pstmt.executeBatch(); // insert remaining
                log.info("Successfully processed and stored {} records in groww_scrip_master.", count);
            }

        } catch (Exception e) {
            log.error("Database error while storing Groww Scrip Master", e);
        }
    }
}
