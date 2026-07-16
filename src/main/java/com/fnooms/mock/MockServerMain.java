package com.fnooms.mock;

import com.fnooms.algo.StrategyConfig;
import com.fnooms.algo.StrategyConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MockServerMain {
    private static final Logger log = LoggerFactory.getLogger(MockServerMain.class);

    public static void main(String[] args) {
        log.info("Starting Mock Data Publisher Server...");

        List<StrategyConfig> configs = StrategyConfigLoader.loadConfigs();

        int port = 8082;
        MockDataPublisherServer server = new MockDataPublisherServer(port, configs);

        server.start();

        log.info("Mock WebSocket Server is listening on ws://localhost:{}", port);

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                log.info("Shutting down Mock Server...");
                server.stop();
            } catch (InterruptedException e) {
                log.error("Failed to stop gracefully", e);
            }
        }));
    }
}
