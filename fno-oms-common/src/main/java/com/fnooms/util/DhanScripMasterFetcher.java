package com.fnooms.util;

import com.fnooms.dao.AlgoKeyValueDAO;
import com.fnooms.dao.DatabaseManager;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DhanScripMasterFetcher {
    private static final Logger log = LoggerFactory.getLogger(DhanScripMasterFetcher.class);
    private static final String SCRIP_MASTER_URL = "https://images.dhan.co/api-data/api-scrip-master.csv";

    public static void fetchAndStoreScripMaster() {
        AlgoKeyValueDAO dao = new AlgoKeyValueDAO();
        String todayDate = java.time.LocalDate.now().toString();
        String lastFetchDate = dao.getValue("dhan.scrip.master.date");

        if (todayDate.equals(lastFetchDate)) {
            log.info("Dhan scrip master table is up to date (fetched on {}). Skipping download.", lastFetchDate);
            return;
        }

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url(SCRIP_MASTER_URL)
                .get()
                .build();

        log.info("Starting download of Dhan Scrip Master from: {}", SCRIP_MASTER_URL);

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("Failed to download Dhan Scrip Master. HTTP {}", response.code());
                return;
            }

            if (response.body() == null) {
                log.error("Empty response body from Dhan Scrip Master API.");
                return;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().byteStream()));
                 Connection conn = DatabaseManager.getInstance().getConnection()) {

                String sql = "INSERT INTO dhan_scrip_master (SEM_EXM_EXCH_ID, SEM_SEGMENT, SEM_SMST_SECURITY_ID, SEM_INSTRUMENT_NAME, " +
                             "SEM_TRADING_SYMBOL, SEM_CUSTOM_SYMBOL, SEM_EXPIRY_DATE, SEM_EXPIRY_FLAG, SEM_STRIKE_PRICE, " +
                             "SEM_OPTION_TYPE, SEM_TICK_SIZE, SEM_LOT_SIZE, SEM_TRADING_SESSION, SEM_EXACT_EXPIRY_DATE, " +
                             "SEM_FREEZE_QUANTITY, SEM_SERIES, SEM_INSTRUMENT_TYPE, SEM_INSTRUMENT_TYPE_NAME, SEM_ISIN) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                             "ON CONFLICT(SEM_SMST_SECURITY_ID) DO UPDATE SET " +
                             "SEM_EXM_EXCH_ID=EXCLUDED.SEM_EXM_EXCH_ID, SEM_SEGMENT=EXCLUDED.SEM_SEGMENT, SEM_INSTRUMENT_NAME=EXCLUDED.SEM_INSTRUMENT_NAME, " +
                             "SEM_TRADING_SYMBOL=EXCLUDED.SEM_TRADING_SYMBOL, SEM_CUSTOM_SYMBOL=EXCLUDED.SEM_CUSTOM_SYMBOL, " +
                             "SEM_EXPIRY_DATE=EXCLUDED.SEM_EXPIRY_DATE, SEM_EXPIRY_FLAG=EXCLUDED.SEM_EXPIRY_FLAG, SEM_STRIKE_PRICE=EXCLUDED.SEM_STRIKE_PRICE, " +
                             "SEM_OPTION_TYPE=EXCLUDED.SEM_OPTION_TYPE, SEM_TICK_SIZE=EXCLUDED.SEM_TICK_SIZE, SEM_LOT_SIZE=EXCLUDED.SEM_LOT_SIZE, " +
                             "SEM_TRADING_SESSION=EXCLUDED.SEM_TRADING_SESSION, SEM_EXACT_EXPIRY_DATE=EXCLUDED.SEM_EXACT_EXPIRY_DATE, " +
                             "SEM_FREEZE_QUANTITY=EXCLUDED.SEM_FREEZE_QUANTITY, SEM_SERIES=EXCLUDED.SEM_SERIES, " +
                             "SEM_INSTRUMENT_TYPE=EXCLUDED.SEM_INSTRUMENT_TYPE, SEM_INSTRUMENT_TYPE_NAME=EXCLUDED.SEM_INSTRUMENT_TYPE_NAME, SEM_ISIN=EXCLUDED.SEM_ISIN;";

                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    int count = 0;
                    conn.setAutoCommit(false);
                    
                    String line;
                    boolean isFirstLine = true;
                    
                    while ((line = reader.readLine()) != null) {
                        if (isFirstLine) {
                            isFirstLine = false;
                            continue; // Skip header
                        }
                        
                        String[] cols = line.split(",", -1);
                        if (cols.length < 16) continue;
                        
                        try {
                            pstmt.setString(1, cols[0]); // SEM_EXM_EXCH_ID
                            pstmt.setString(2, cols[1]); // SEM_SEGMENT
                            pstmt.setString(3, cols[2]); // SEM_SMST_SECURITY_ID
                            pstmt.setString(4, cols[3]); // SEM_INSTRUMENT_NAME
                            pstmt.setString(5, cols[5]); // SEM_TRADING_SYMBOL
                            pstmt.setString(6, cols[7]); // SEM_CUSTOM_SYMBOL
                            pstmt.setString(7, cols[8]); // SEM_EXPIRY_DATE
                            pstmt.setString(8, cols[12]); // SEM_EXPIRY_FLAG
                            pstmt.setDouble(9, parseDoubleSafely(cols[9])); // SEM_STRIKE_PRICE
                            pstmt.setString(10, cols[10]); // SEM_OPTION_TYPE
                            pstmt.setDouble(11, parseDoubleSafely(cols[11])); // SEM_TICK_SIZE
                            pstmt.setInt(12, parseIntSafely(cols[6])); // SEM_LOT_SIZE
                            pstmt.setString(13, ""); // SEM_TRADING_SESSION
                            pstmt.setString(14, ""); // SEM_EXACT_EXPIRY_DATE
                            pstmt.setInt(15, 0); // SEM_FREEZE_QUANTITY
                            pstmt.setString(16, cols[14]); // SEM_SERIES
                            pstmt.setInt(17, 0); // SEM_INSTRUMENT_TYPE
                            pstmt.setString(18, cols[13]); // SEM_INSTRUMENT_TYPE_NAME
                            pstmt.setString(19, ""); // SEM_ISIN
                            
                            pstmt.addBatch();
                            count++;

                            if (count % 5000 == 0) {
                                pstmt.executeBatch();
                                conn.commit();
                                log.info("Inserted/Updated {} Dhan records...", count);
                            }
                        } catch (Exception ex) {
                            log.warn("Error parsing scrip row: {}", line, ex);
                        }
                    }

                    pstmt.executeBatch();
                    conn.commit();
                    dao.setValue("dhan.scrip.master.date", todayDate);
                    log.info("Successfully processed and stored {} records in dhan_scrip_master.", count);
                } finally {
                    conn.setAutoCommit(true);
                }
            }
        } catch (Exception e) {
            log.error("Error fetching or storing Dhan Scrip Master: ", e);
        }
    }

    private static double parseDoubleSafely(String val) {
        if (val == null || val.trim().isEmpty()) return 0.0;
        try {
            return Double.parseDouble(val.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static int parseIntSafely(String val) {
        if (val == null || val.trim().isEmpty()) return 0;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
