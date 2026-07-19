package com.fnooms.mock;

import com.fnooms.algo.StrategyConfig;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockDataPublisherServer extends WebSocketServer {
    private static final Logger log = LoggerFactory.getLogger(MockDataPublisherServer.class);
    private final MockDataGenerator generator;
    private ScheduledExecutorService scheduler;
    private final List<String> activeTokens = new ArrayList<>();
    
    public MockDataPublisherServer(int port, List<StrategyConfig> configs) {
        super(new InetSocketAddress(port));
        this.generator = new MockDataGenerator();
        if (configs != null) {
            this.activeTokens.addAll(configs.stream()
                .map(StrategyConfig::getToken)
                .filter(t -> t != null)
                .collect(Collectors.toList()));
        }
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        log.info("New mock client connected: {}", conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        log.info("Mock client disconnected: {} with reason: {}", conn.getRemoteSocketAddress(), reason);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        log.info("Received message from client: {}", message);
        // We can parse subscription messages here if needed, but for now we just broadcast active tokens.
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        log.info("Received binary message from client: {}", message.remaining());
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        log.error("Error on connection {}", conn != null ? conn.getRemoteSocketAddress() : "null", ex);
    }

    @Override
    public void onStart() {
        log.info("MockDataPublisherServer started successfully on port {}", getPort());
        startPublishing();
    }
    
    private void startPublishing() {
        if (activeTokens.isEmpty()) {
            log.warn("MockDataPublisherServer: No active tokens to publish for!");
            // We can add dummy tokens if we want, but usually it gets passed from config
            activeTokens.add("26000"); // default token if empty just to keep alive
        }

        scheduler = Executors.newSingleThreadScheduledExecutor();
        
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (getConnections().isEmpty()) {
                    return; // Don't generate/send if no one is listening
                }

                generator.reloadConfig();
                
                // MStock requires the full batched buffer 
                // BUT in type B, it reads the 379 packets sequentially from the buffer.
                ByteBuffer batchBuffer = ByteBuffer.allocate(379 * activeTokens.size());
                
                for (String token : activeTokens) {
                    double price = generator.getNextPrice(token);
                    ByteBuffer packet = generator.createMStockPacket(token, price);
                    
                    batchBuffer.put(packet);
                    packet.rewind();
                }
                
                batchBuffer.flip();
                broadcast(batchBuffer);

            } catch (Exception e) {
                log.error("Error in mock data generation loop", e);
            }
        }, 1, 1, TimeUnit.SECONDS);
    }
    
    @Override
    public void stop() throws InterruptedException {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        super.stop();
    }
}
