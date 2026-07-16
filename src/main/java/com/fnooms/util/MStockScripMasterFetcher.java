package com.fnooms.util;

import com.fnooms.dao.AlgoKeyValueDAO;
import com.fnooms.dao.DatabaseManager;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.TimeUnit;

public class MStockScripMasterFetcher {
    private static final Logger log = LoggerFactory.getLogger(MStockScripMasterFetcher.class);
    private static final String SCRIP_MASTER_URL = "https://api.mstock.trade/openapi/typeb/instruments/OpenAPIScripMaster";

    public static void fetchAndStoreScripMaster() {
        AlgoKeyValueDAO dao = new AlgoKeyValueDAO();
        String apiKey = dao.getValue("mstock.api_key");
        String jwtToken = dao.getValue("mstock.jwt_token");
        String todayDate = java.time.LocalDate.now().toString();
        String lastFetchDate = dao.getValue("mstock.scrip.master.date");

        if (todayDate.equals(lastFetchDate)) {
            log.info("mStock scrip master table is up to date (fetched on {}). Skipping download.", lastFetchDate);
            return;
        }

        if (apiKey == null || jwtToken == null) {
            log.error("Cannot fetch mStock Scrip Master: mstock.api_key or mstock.jwt_token is missing in DB.");
            return;
        }

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url(SCRIP_MASTER_URL)
                .get()
                .addHeader("X-Mirae-Version", "1")
                .addHeader("X-PrivateKey", apiKey)
                .addHeader("Authorization", "Bearer " + jwtToken)
                .build();

        log.info("Starting download of mStock Scrip Master from: {}", SCRIP_MASTER_URL);

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("Failed to download mStock Scrip Master. HTTP {}: {}", response.code(), response.body() != null ? response.body().string() : "");
                return;
            }

            if (response.body() == null) {
                log.error("Empty response body from mStock Scrip Master API.");
                return;
            }

            try (Reader reader = new InputStreamReader(response.body().byteStream());
                 Connection conn = DatabaseManager.getInstance().getConnection()) {

                Gson gson = new Gson();
                JsonArray array = gson.fromJson(reader, JsonArray.class);
                if (array == null || array.isEmpty()) {
                    log.error("Empty JSON array received.");
                    return;
                }

                String sql = "INSERT INTO mstock_scrip_master (instrument_token, instrument_name, tradingsymbol, expiry_date, strike_price, option_type, exchange_segment) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                             "ON CONFLICT(instrument_token) DO UPDATE SET " +
                             "instrument_name=EXCLUDED.instrument_name, tradingsymbol=EXCLUDED.tradingsymbol, expiry_date=EXCLUDED.expiry_date, strike_price=EXCLUDED.strike_price, option_type=EXCLUDED.option_type, exchange_segment=EXCLUDED.exchange_segment;";

                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    int count = 0;
                    conn.setAutoCommit(false);
                    
                    for (JsonElement element : array) {
                        try {
                            JsonObject obj = element.getAsJsonObject();
                            String token = obj.has("token") && !obj.get("token").isJsonNull() ? obj.get("token").getAsString() : "";
                            String symbol = obj.has("symbol") && !obj.get("symbol").isJsonNull() ? obj.get("symbol").getAsString() : "";
                            String name = obj.has("name") && !obj.get("name").isJsonNull() ? obj.get("name").getAsString() : "";
                            String expiry = obj.has("expiry") && !obj.get("expiry").isJsonNull() ? obj.get("expiry").getAsString() : "";
                            String strike = obj.has("strike") && !obj.get("strike").isJsonNull() ? obj.get("strike").getAsString() : "";
                            String lotSize = obj.has("lotsize") && !obj.get("lotsize").isJsonNull() ? obj.get("lotsize").getAsString() : "";
                            String instType = obj.has("instrumenttype") && !obj.get("instrumenttype").isJsonNull() ? obj.get("instrumenttype").getAsString() : "";
                            String exchSeg = obj.has("exch_seg") && !obj.get("exch_seg").isJsonNull() ? obj.get("exch_seg").getAsString() : "";
                            String tickSize = obj.has("tick_size") && !obj.get("tick_size").isJsonNull() ? obj.get("tick_size").getAsString() : "";

                            pstmt.setString(1, token);
                            pstmt.setString(2, symbol); // 'symbol' in JSON is instrument_name (e.g. NIFTY)
                            pstmt.setString(3, name); // 'name' in JSON is tradingsymbol (e.g. NIFTY21JUL26...)
                            pstmt.setString(4, expiry);
                            pstmt.setDouble(5, parseDoubleSafely(strike));
                            pstmt.setString(6, instType); // option_type
                            pstmt.setString(7, exchSeg); // exchange_segment

                            pstmt.addBatch();
                            count++;

                            if (count % 5000 == 0) {
                                pstmt.executeBatch();
                                conn.commit();
                                log.info("Inserted/Updated {} records...", count);
                            }
                        } catch (Exception ex) {
                            log.warn("Error parsing scrip object: {}", element, ex);
                        }
                    }

                    pstmt.executeBatch();
                    conn.commit();
                    dao.setValue("mstock.scrip.master.date", todayDate, "SYSTEM");
                    log.info("Successfully processed and stored {} records in mstock_scrip_master.", count);
                } finally {
                    conn.setAutoCommit(true);
                }
            }
        } catch (Exception e) {
            log.error("Error fetching or storing mStock Scrip Master: ", e);
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
