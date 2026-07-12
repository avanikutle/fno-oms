package com.fnooms.broker;

import com.fnooms.broker.mstock.MStockBrokerClient;
import com.fnooms.dao.BrokerConfigDAO;
import com.fnooms.model.BrokerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory that returns the currently active BrokerClient implementation.
 *
 * <p>Reads the active broker config from the database and instantiates
 * the appropriate client. Cached per request cycle — DB is only hit
 * when the active broker changes.
 */
public class BrokerClientFactory {

    private static final Logger log = LoggerFactory.getLogger(BrokerClientFactory.class);
    private static final BrokerConfigDAO configDAO = new BrokerConfigDAO();

    private BrokerClientFactory() {}

    /**
     * Returns a fully initialised BrokerClient for the currently active broker.
     *
     * @throws IllegalStateException if no active broker is configured
     */
    public static BrokerClient getActiveClient() {
        BrokerConfig config = configDAO.getActive();
        if (config == null) {
            throw new IllegalStateException(
                    "No active broker configured. Please go to Settings and add a broker.");
        }
        return buildClient(config);
    }

    /**
     * Returns a BrokerClient for a specific config (e.g. for test-connection calls).
     */
    public static BrokerClient getClientFor(BrokerConfig config) {
        return buildClient(config);
    }

    private static BrokerClient buildClient(BrokerConfig config) {
        BrokerType type;
        try {
            type = BrokerType.fromString(config.getBrokerType());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Unsupported broker type: " + config.getBrokerType(), e);
        }

        switch (type) {
            case MSTOCK:
                log.debug("Creating MStockBrokerClient for config id={}", config.getId());
                return new MStockBrokerClient(config);

            // Future integrations — add cases here:
            // case ZERODHA:
            //     return new ZerodhaBrokerClient(config);
            // case UPSTOX:
            //     return new UpstoxBrokerClient(config);

            default:
                throw new IllegalStateException("No implementation for broker: " + type);
        }
    }
}
