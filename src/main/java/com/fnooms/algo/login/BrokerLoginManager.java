package com.fnooms.algo.login;

import com.fnooms.dao.AlgoKeyValueDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrokerLoginManager {

    private static final Logger log = LoggerFactory.getLogger(BrokerLoginManager.class);
    private final AlgoKeyValueDAO dao = new AlgoKeyValueDAO();

    /**
     * Attempts to login to the specified broker using stored credentials and a user-provided TOTP code.
     * On success, the JWT token is saved to the database.
     *
     * @param brokerType "MSTOCK" or "ANGELONE"
     * @param totp       The 6-digit code provided by the user via UI
     * @return true if successful, false otherwise
     */
    public boolean performWebLogin(String brokerPrefix, String totp) {
        try {
            String prefix = brokerPrefix.toLowerCase();
            if (prefix.startsWith("mstock")) {
                String userId = dao.getValue(prefix + ".userid");
                String password = dao.getValue(prefix + ".pdcred");
                String apiKey = dao.getValue(prefix + ".api_key");

                if (userId == null || password == null || apiKey == null) {
                    throw new IllegalStateException("Missing mStock credentials in algo_key_value table for prefix: " + prefix);
                }

                MStockLogin loginImpl = new MStockLogin();
                String[] tokens = loginImpl.login(userId, password, totp, apiKey);
                
                dao.setValue(prefix + ".jwt_token", tokens[0], "WEB_LOGIN");
                dao.setValue(prefix + ".refresh_token", tokens[1], "WEB_LOGIN");
                log.info("Successfully updated mStock tokens in database for prefix {}.", prefix);
                return true;

            } else if (prefix.startsWith("angelone")) {
                String userId = dao.getValue(prefix + ".client_code");
                String password = dao.getValue(prefix + ".pdcred");
                String apiKey = dao.getValue(prefix + ".api_key");

                if (userId == null || password == null || apiKey == null) {
                    throw new IllegalStateException("Missing AngelOne credentials in algo_key_value table for prefix: " + prefix);
                }

                AngelOneLogin loginImpl = new AngelOneLogin();
                String[] tokens = loginImpl.login(userId, password, totp, apiKey);

                dao.setValue(prefix + ".jwt_token", tokens[0], "WEB_LOGIN");
                dao.setValue(prefix + ".feed_token", tokens[1], "WEB_LOGIN");
                log.info("Successfully updated AngelOne JWT token in database for prefix {}.", prefix);
                return true;

            } else {
                throw new IllegalArgumentException("Unsupported broker type for login: " + prefix);
            }
        } catch (Exception e) {
            log.error("Login failed for broker prefix: " + brokerPrefix, e);
            return false;
        }
    }
}
