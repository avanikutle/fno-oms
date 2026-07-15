package com.fnooms.algo;

import com.fnooms.service.OrderService;
import com.fnooms.dao.AlgoKeyValueDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AlgoOrchestratorMain {
    private static final Logger log = LoggerFactory.getLogger(AlgoOrchestratorMain.class);

    public static void main(String[] args) {
        log.info("Starting Algo Orchestrator...");

        AlgoKeyValueDAO kvDao = new AlgoKeyValueDAO();

        // Ensure default properties exist in DB if not already set
        if (kvDao.getValue("algo.feedBroker") == null) {
            kvDao.setValue("algo.feedBroker", "MSTOCK", "SYSTEM");
        }
        if (kvDao.getValue("algo.orderBroker") == null) {
            kvDao.setValue("algo.orderBroker", "MSTOCK", "SYSTEM");
        }

        String feedBroker = kvDao.getValue("algo.feedBroker");
        String orderBroker = kvDao.getValue("algo.orderBroker");

        log.info("Global Configuration -> Feed Broker: {}, Order Broker: {}", feedBroker, orderBroker);

        // 1. Load Scrip Master into memory
        ScripMasterService.loadScripMaster();

        // 2. Load Configurations (Symbol level strategies from properties)
        List<StrategyConfig> configs = StrategyConfigLoader.loadConfigs("strategy.properties");

        if (configs.isEmpty()) {
            log.error("No active strategies found in strategy.properties.");
            return;
        }

        OrderService orderService = new OrderService();

        // 3. Initialize Strategy Engine with target order broker
        StrategyEngine engine = new StrategyEngine(orderService, orderBroker);
        for (StrategyConfig config : configs) {
            engine.addConfig(config);
        }

        // 4. Spawn the respective Market Data Listener based on feedBroker
        MarketDataListener listener = null;

        if ("ANGELONE".equalsIgnoreCase(feedBroker)) {
            listener = new AngelOneDataListener(engine, configs);
        } else if ("MSTOCK".equalsIgnoreCase(feedBroker)) {
            listener = new MstockDataListener(engine, configs);
        } else {
            log.error("Unsupported feed broker: {}", feedBroker);
            return;
        }

        // Start listening (this will block the main thread, or we can run in a new
        // Thread if we add more listeners)
        listener.start();
    }
}
