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
    public boolean performWebLogin(String brokerType, String totp) {
        try {
            if ("MSTOCK".equalsIgnoreCase(brokerType)) {
                String userId = dao.getValue("mstock.userid");
                String password = dao.getValue("mstock.pdcred");
                String apiKey = dao.getValue("mstock.api_key");

                if (userId == null || password == null || apiKey == null) {
                    throw new IllegalStateException("Missing mStock credentials in algo_key_value table");
                }

                MStockLogin loginImpl = new MStockLogin();
                String[] tokens = loginImpl.login(userId, password, totp, apiKey);
                
                dao.setValue("mstock.jwt_token", tokens[0], "WEB_LOGIN");
                dao.setValue("mstock.refresh_token", tokens[1], "WEB_LOGIN");
                log.info("Successfully updated mStock tokens in database.");
                return true;

            } else if ("ANGELONE".equalsIgnoreCase(brokerType)) {
                String userId = dao.getValue("angelone.client_code");
                String password = dao.getValue("angelone.pdcred");
                String apiKey = dao.getValue("angelone.api_key");

                if (userId == null || password == null || apiKey == null) {
                    throw new IllegalStateException("Missing AngelOne credentials in algo_key_value table");
                }

                AngelOneLogin loginImpl = new AngelOneLogin();
                String[] tokens = loginImpl.login(userId, password, totp, apiKey);

                dao.setValue("angelone.jwt_token", tokens[0], "WEB_LOGIN");
                dao.setValue("angelone.feed_token", tokens[1], "WEB_LOGIN");
                log.info("Successfully updated AngelOne JWT token in database.");
                return true;

            } else {
                throw new IllegalArgumentException("Unsupported broker type for login: " + brokerType);
            }
        } catch (Exception e) {
            log.error("Login failed for broker: " + brokerType, e);
            return false;
        }
    }
}
