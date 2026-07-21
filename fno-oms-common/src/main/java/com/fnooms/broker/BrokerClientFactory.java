package com.fnooms.broker;

import com.fnooms.amock.MockBrokerClient;
import com.fnooms.broker.angelone.AngelOneBrokerClient;
import com.fnooms.broker.mstock.MStockBrokerClient;
import com.fnooms.dao.AlgoKeyValueDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        return buildClient(activeType.toLowerCase());
    }

    public static BrokerClient getClientFor(String brokerPrefix) {
        return buildClient(brokerPrefix.toLowerCase());
    }

    private static BrokerClient buildClient(String prefix) {
        if (prefix.startsWith("mstock")) {
            log.debug("Creating MStockBrokerClient for prefix {}", prefix);
            return new MStockBrokerClient(prefix);
        } else if (prefix.startsWith("angelone")) {
            log.debug("Creating AngelOneBrokerClient for prefix {}", prefix);
            return new AngelOneBrokerClient(prefix);
        } else if (prefix.startsWith("dhan")) {
            log.debug("Creating DhanBrokerClient for prefix {}", prefix);
            return new com.fnooms.broker.dhan.DhanBrokerClient(prefix);
        } else if (prefix.startsWith("mock")) {
            log.debug("Creating MockBrokerClient for prefix {}", prefix);
            return new MockBrokerClient(prefix);
        } else {
            throw new IllegalStateException("No implementation for broker prefix: " + prefix);
        }
    }
}
