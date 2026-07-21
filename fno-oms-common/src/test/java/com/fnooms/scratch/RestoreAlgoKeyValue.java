package com.fnooms.scratch;

import com.fnooms.dao.DatabaseManager;
import java.sql.Connection;
import java.sql.Statement;

public class RestoreAlgoKeyValue {
    public static void main(String[] args) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {

            System.out.println("Recreating algo_key_value table if not exists...");
            String createTableSql = "CREATE TABLE IF NOT EXISTS algo_key_value (" +
                    "key_name VARCHAR(100) PRIMARY KEY, " +
                    "key_value TEXT NOT NULL, " +
                    "updated_by VARCHAR(50) DEFAULT 'SYSTEM', " +
                    "updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()" +
                    ")";
            stmt.execute(createTableSql);

            String createTriggerSql = "CREATE OR REPLACE TRIGGER trg_algo_key_value_updated_at " +
                    "BEFORE UPDATE ON algo_key_value " +
                    "FOR EACH ROW EXECUTE FUNCTION update_updated_at_column()";
            stmt.execute(createTriggerSql);

            System.out.println("Inserting default keys...");
            String[] insertSqls = {
                "INSERT INTO algo_key_value (key_name, key_value, updated_by) VALUES ('angelone.scrip.master.date', '1970-01-01', 'SYSTEM') ON CONFLICT (key_name) DO NOTHING;",
                "INSERT INTO algo_key_value (key_name, key_value, updated_by) VALUES ('algo.feedBroker', 'MSTOCK', 'SYSTEM') ON CONFLICT (key_name) DO NOTHING;",
                "INSERT INTO algo_key_value (key_name, key_value, updated_by) VALUES ('algo.orderBroker', 'MSTOCK', 'SYSTEM') ON CONFLICT (key_name) DO NOTHING;",
                "INSERT INTO algo_key_value (key_name, key_value, updated_by) VALUES ('mock.price.min', '1000', 'SYSTEM') ON CONFLICT (key_name) DO NOTHING;",
                "INSERT INTO algo_key_value (key_name, key_value, updated_by) VALUES ('mock.price.max', '2000', 'SYSTEM') ON CONFLICT (key_name) DO NOTHING;",
                "INSERT INTO algo_key_value (key_name, key_value, updated_by) VALUES ('mock.price.volatility', '5', 'SYSTEM') ON CONFLICT (key_name) DO NOTHING;",
                "INSERT INTO algo_key_value (key_name, key_value, updated_by) VALUES ('dhan.client_id', 'DHAN_CLIENT_ID', 'SYSTEM') ON CONFLICT (key_name) DO NOTHING;",
                "INSERT INTO algo_key_value (key_name, key_value, updated_by) VALUES ('dhan.api_key', 'DHAN_API_KEY', 'SYSTEM') ON CONFLICT (key_name) DO NOTHING;",
                "INSERT INTO algo_key_value (key_name, key_value, updated_by) VALUES ('dhan.api_secret', 'DHAN_API_SECRET', 'SYSTEM') ON CONFLICT (key_name) DO NOTHING;",
                "INSERT INTO algo_key_value (key_name, key_value, updated_by) VALUES ('dhan.scrip.master.date', '1970-01-01', 'SYSTEM') ON CONFLICT (key_name) DO NOTHING;",
                "INSERT INTO algo_key_value (key_name, key_value) VALUES ('angelone.api_key', 'Cs4E2TqP') ON CONFLICT (key_name) DO NOTHING;",
                "INSERT INTO algo_key_value (key_name, key_value) VALUES ('angelone.client_code', 'S54534677') ON CONFLICT (key_name) DO NOTHING;",
                "INSERT INTO algo_key_value (key_name, key_value) VALUES ('angelone.jwt_token', 'eyJhbGciOiJIUzUxMiJ9.eyJ1c2VybmFtZSI6IlM1NDUzNDY3NyIsInJvbGVzIjowLCJ1c2VydHlwZSI6IlVTRVIiLCJ0b2tlbiI6ImV5SmhiR2NpT2lKU1V6STFOaUlzSW5SNWNDSTZJa3BYVkNKOS5leUoxYzJWeVgzUjVjR1VpT2lKamJHbGxiblFpTENKMGIydGxibDkwZVhCbElqb2lkSEpoWkdWZllXTmpaWE56WDNSdmEyVnVJaXdpWjIxZmFXUWlPakV4TENKemIzVnlZMlVpT2lJeklpd2laR1YyYVdObFgybGtJam9pTWpNNFpUUmhOVFl0WmpBd09DMHpOelF5TFdGbE1XSXRObVZrWWpRNVl6Y3daVFl6SWl3aWEybGtJam9pZEhKaFpHVmZhMlY1WDNZeUlpd2liMjF1WlcxaGJtRm5aWEpwWkNJNk1URXNJbkJ5YjJSMVkzUnpJanA3SW1SbGJXRjBJanA3SW5OMFlYUjFjeUk2SW1GamRHbDJaU0o5TENKdFppSTZleUp6ZEdGMGRYTWlPaUpoWTNScGRtVWlmU3dpYm1KMVRHVnVaR2x1WnlJNmV5SnpkR0YwZFhNaU9pSmhZM1JwZG1VaWZYMHNJbWx6Y3lJNkluUnlZV1JsWDJ4dloybHVYM05sY25acFkyVWlMQ0p6ZFdJaU9pSlROVFExTXpRMk56Y2lMQ0psZUhBaU9qRTNPRFF3T0RZek56TXNJbTVpWmlJNk1UYzRNems1T1RjNU15d2lhV0YwSWpveE56ZzPVGs1TnprekxDSnFkR2tpT2lKa056RmlNakF5T1Mxak1XTmlMVFF5WldVdFlqYzFOUzAzTVdFelpXVTVaVFptTldVaUxDSlViMnRsYmlJNklpSjkuTHhMS1dmM2hZUUtOZDdXcXBtR2hYdnNzRW5PWmlOdG00bHhJc0t6aV92YlMxNU1sbFFfVWhTbmFFalVCM2hDLTd3TlR2cXRUTk9mMG42SkVma0tqZnBFRndwbDNtaV9vN1RxdVpYZ1laUGF0MFEyS3RKSnlRR19LUWNfNG53SERBaG5rd3VFdGxQU21mZGtDTlZIRzFMWUdaSnFVeGhDYlJ3UG5aQXFSTjc4IiwiQVBJLUtFWSI6IkNzNEUyVHFQIiwiaWF0IjoxNzgzOTk5OTczLCJleHAiOjE3ODQwNTM4MDB9._yqaBT1S50JyXwces4awGiSOKVKw9BD9p1B20_piPbyiicYhGTEm1-PNh4lklOlBLRpMMM_8ELS8qAI0MczmGA') ON CONFLICT (key_name) DO NOTHING;",
                "INSERT INTO algo_key_value (key_name, key_value) VALUES ('angelone.feed_token', 'eyJhbGciOiJIUzUxMiJ9.eyJ1c2VybmFtZSI6IlM1NDUzNDY3NyIsImlhdCI6MTc4Mzk5OTk3MywiZXhwIjoxNzg0MDg2MzczfQ.Sul2m8izu01SCgQfGLL0_mzfs6u08bqj_1R8W6gJYKeMfVncAOVbw0hV6b-JhOgmpWiUfxQFZAGlXHC3KSxddg') ON CONFLICT (key_name) DO NOTHING;",
                "INSERT INTO algo_key_value (key_name, key_value) VALUES ('app.watchlist.basesymbols', 'NIFTY,BANKNIFTY') ON CONFLICT (key_name) DO NOTHING;",
                "INSERT INTO algo_key_value (key_name, key_value) VALUES ('algo.trailing.sl.mode', 'continuous') ON CONFLICT (key_name) DO NOTHING;"
            };

            for (String sql : insertSqls) {
                try {
                    stmt.execute(sql);
                } catch (Exception e) {
                    System.out.println("Warning: Could not insert key. " + e.getMessage());
                }
            }
            
            System.out.println("algo_key_value table recreated and populated!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
