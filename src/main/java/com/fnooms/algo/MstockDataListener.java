package com.fnooms.algo;

import com.fnooms.util.CredsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

public class MstockDataListener implements MarketDataListener {
    private static final Logger log = LoggerFactory.getLogger(MstockDataListener.class);
    private static final String ws_url = CredsUtil.getWsCreds();

    private final StrategyEngine engine;
    private final List<StrategyConfig> configs;

    public MstockDataListener(StrategyEngine engine, List<StrategyConfig> configs) {
        this.engine = engine;
        this.configs = configs;
    }

    @Override
    public void start() {
        if (configs.isEmpty()) {
            log.error("MstockDataListener: No active strategies found to subscribe to.");
            return;
        }

        int count = 0;
        while (true) {
            try {
                log.info("Connecting MStock WS. Iteration count {}", count++);
                connectAndListen();
            } catch (Exception e) {
                log.error("Reconnect after error: {}", e.getMessage());
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void connectAndListen() {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        client.newWebSocketBuilder()
                .buildAsync(URI.create(ws_url), new WebSocket.Listener() {
                    @Override
                    public void onOpen(WebSocket webSocket) {
                        log.info("MStock WebSocket Connected");
                        webSocket.request(1);

                        // Send login message to maintain session
                        webSocket.sendText("LOGIN:" + CredsUtil.getAccessToken(), true);

                        // Build dynamic subscription payload based on configured tokens
                        List<String> tokens = configs.stream()
                                .map(StrategyConfig::getToken)
                                .filter(t -> t != null)
                                .collect(Collectors.toList());

                        if (!tokens.isEmpty()) {
                            String tokenArrayStr = String.join(",", tokens);
                            String subscribeMsg = String.format("{\"a\": \"subscribe\", \"v\": [%s]}", tokenArrayStr);
                            log.info("Subscribing on MStock: {}", subscribeMsg);
                            webSocket.sendText(subscribeMsg, true);
                        }
                    }

                    private void parseMarketData(ByteBuffer data) {
                        data.order(ByteOrder.BIG_ENDIAN);
                        int length = data.remaining();
                        if (length < 8) return; 

                        short packetCount = data.getShort();
                        short packetLength = data.getShort();

                        for (int i = 0; i < packetCount; i++) {
                            if (data.remaining() < 4) break;

                            int token = data.getInt();
                            int rawLtp = data.getInt();
                            double ltp = rawLtp / 100.0;

                            // Resolve symbol from token using ScripMaster
                            String symbol = ScripMasterService.getSymbol(String.valueOf(token));
                            if (symbol != null) {
                                // Feed the price to the Strategy Engine
                                engine.onPriceUpdate(symbol, ltp);
                            }

                            int remainingPacketBytes = packetLength - 8; 
                            if (remainingPacketBytes > 0 && data.remaining() >= remainingPacketBytes) {
                                data.position(data.position() + remainingPacketBytes);
                            }
                        }
                    }

                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        log.info("Text msg received: {}", data);
                        webSocket.request(1);
                        return null;
                    }

                    @Override
                    public CompletionStage<?> onBinary(WebSocket webSocket, java.nio.ByteBuffer data, boolean last) {
                        parseMarketData(data);
                        webSocket.request(1);
                        return null;
                    }

                    @Override
                    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                        log.info("MStock WebSocket Closed: {} / {}", statusCode, reason);
                        return null;
                    }

                    @Override
                    public void onError(WebSocket webSocket, Throwable error) {
                        log.error("MStock WebSocket Error: {}", error.getMessage());
                    }
                }).join();

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
