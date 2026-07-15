package com.fnooms.broker;

import com.fnooms.broker.mstock.MStockBrokerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fnooms.dao.AlgoKeyValueDAO;

/**
 * Factory that returns the currently active BrokerClient implementation.
 */
public class BrokerClientFactory {

    private static final Logger log = LoggerFactory.getLogger(BrokerClientFactory.class);
    private static final AlgoKeyValueDAO dao = new AlgoKeyValueDAO();

    private BrokerClientFactory() {}

    /**
     * Returns a fully initialised BrokerClient for the currently active broker.
     */
    public static BrokerClient getActiveClient() {
        // Here you can use algo_key_value to decide the default broker.
        // For now, we will default to MSTOCK as it's the primary execution broker.
        String activeType = dao.getValue("algo.activeBroker");
        if (activeType == null) {
            activeType = "MSTOCK"; // Fallback
        }
        return buildClient(BrokerType.fromString(activeType));
    }

    public static BrokerClient getClientFor(String brokerTypeStr) {
        return buildClient(BrokerType.fromString(brokerTypeStr));
    }

    private static BrokerClient buildClient(BrokerType type) {
        switch (type) {
            case MSTOCK:
                log.debug("Creating MStockBrokerClient");
                return new MStockBrokerClient();
            default:
                throw new IllegalStateException("No implementation for broker: " + type);
        }
    }
}
