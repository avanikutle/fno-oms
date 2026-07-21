package com.fnooms.amock;

import com.fnooms.algo.BinaryParserUtil;
import com.fnooms.algo.MarketDataListener;
import com.fnooms.algo.StrategyConfig;
import com.fnooms.algo.StrategyEngine;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockMarketDataListener implements MarketDataListener {
    private static final Logger log = LoggerFactory.getLogger(MockMarketDataListener.class);
    
    private final StrategyEngine engine;
    private final List<StrategyConfig> configs;
    private final MockDataGenerator generator;
    
    private ScheduledExecutorService scheduler;
    private volatile boolean running = false;
    private final List<String> activeTokens = new ArrayList<>();

    // Keep all generated packets so we can save them periodically
    private final List<ByteBuffer> allPackets = new ArrayList<>();

    public MockMarketDataListener(StrategyEngine engine, List<StrategyConfig> configs) {
        this.engine = engine;
        this.configs = configs;
        this.generator = new MockDataGenerator();
        
        List<String> tokens = configs.stream()
                .map(StrategyConfig::getToken)
                .filter(t -> t != null)
                .collect(Collectors.toList());
        activeTokens.addAll(tokens);
    }

    @Override
    public void start() {
        if (activeTokens.isEmpty()) {
            log.warn("MockMarketDataListener: No tokens to subscribe to.");
            return;
        }

        running = true;
        scheduler = Executors.newSingleThreadScheduledExecutor();
        
        // Generate a new tick every 1 second
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (!running) return;

                generator.reloadConfig();
                
                ByteBuffer batchBuffer = ByteBuffer.allocate(379 * activeTokens.size());
                
                for (String token : activeTokens) {
                    double price = generator.getNextPrice(token);
                    ByteBuffer packet = generator.createMStockPacket(token, price);
                    
                    // Copy into batch
                    batchBuffer.put(packet);
                    packet.rewind();
                    
                    // Store copy for saving
                    ByteBuffer copy = ByteBuffer.allocate(379);
                    copy.put(packet);
                    copy.rewind();
                    allPackets.add(copy);
                }
                
                batchBuffer.flip();
                
                // Route through our exact binary parser
                BinaryParserUtil.parseMStockMarketData(batchBuffer, engine);

                // Periodically save to file (every ~500 packets)
                if (allPackets.size() >= 500) {
                    generator.savePacketsToFile(new ArrayList<>(allPackets), "src/main/resources/mock/mstock_mock_data.bin");
                    allPackets.clear();
                }

            } catch (Exception e) {
                log.error("Error in mock data generation loop", e);
            }
        }, 1, 1, TimeUnit.SECONDS);

        log.info("MockMarketDataListener started for {} tokens.", activeTokens.size());
    }

    @Override
    public void stop() {
        running = false;
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        if (!allPackets.isEmpty()) {
            generator.savePacketsToFile(allPackets, "src/main/resources/mock/mstock_mock_data.bin");
            allPackets.clear();
        }
        log.info("MockMarketDataListener stopped.");
    }

    @Override
    public void addSubscription(String symbol) {
        // Implement dynamic add if needed for mock
        String token = com.fnooms.algo.ScripMasterService.getInstance().getToken(symbol);
        if (token != null && !activeTokens.contains(token)) {
            activeTokens.add(token);
            log.info("MockMarketDataListener dynamically subscribed to {}", symbol);
        }
    }
}
