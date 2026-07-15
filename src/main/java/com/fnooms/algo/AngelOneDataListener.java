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

public class AngelOneDataListener implements MarketDataListener {
    private static final Logger log = LoggerFactory.getLogger(AngelOneDataListener.class);
    private static final String WS_URL = "wss://smartapisocket.angelone.in/smart-stream";

    private final StrategyEngine engine;
    private final List<StrategyConfig> configs;

    public AngelOneDataListener(StrategyEngine engine, List<StrategyConfig> configs) {
        this.engine = engine;
        this.configs = configs;
    }

    @Override
    public void start() {
        if (configs.isEmpty()) {
            log.error("AngelOneDataListener: No active strategies found to subscribe to.");
            return;
        }

        int count = 0;
        while (true) {
            try {
                log.info("Connecting Angel One WS. Iteration count {}", count++);
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
                .header("Authorization", "Bearer " + CredsUtil.getAngelOneJwtToken())
                .header("x-api-key", CredsUtil.getAngelOneApiKey())
                .header("x-client-code", CredsUtil.getAngelOneClientCode())
                .header("x-feed-token", CredsUtil.getAngelOneFeedToken())
                .buildAsync(URI.create(WS_URL), new WebSocket.Listener() {
                    @Override
                    public void onOpen(WebSocket webSocket) {
                        log.info("Angel One WebSocket Connected");
                        webSocket.request(1);

                        // Build dynamic subscription payload based on configured tokens
                        List<String> tokens = configs.stream()
                                .map(StrategyConfig::getToken)
                                .filter(t -> t != null)
                                .collect(Collectors.toList());

                        if (!tokens.isEmpty()) {
                            // Using Mode 1 (LTP) and ExchangeType 2 (NFO for now, can be mapped from
                            // config)
                            // Ideally, we group tokens by exchange type. For simplicity, treating all as
                            // NSE/NFO (1/2) or MCX (5)
                            // A robust implementation would map exchange strings to exchangeType ints.
                            String tokenArrayStr = tokens.stream().map(t -> "\"" + t + "\"")
                                    .collect(Collectors.joining(","));

                            String subscribeMsg = String.format("""
                                    {
                                      "correlationID": "fnooms-algo",
                                      "action": 1,
                                      "params": {
                                        "mode": 1,
                                        "tokenList": [
                                          {
                                            "exchangeType": 2,
                                            "tokens": [%s]
                                          },
                                          {
                                            "exchangeType": 5,
                                            "tokens": [%s]
                                          }
                                        ]
                                      }
                                    }
                                    """, tokenArrayStr, tokenArrayStr);

                            log.info("Subscribing on Angel One: {}", subscribeMsg);
                            webSocket.sendText(subscribeMsg, true);
                        }
                    }

                    private void parseMarketData(ByteBuffer data) {
                        data.order(ByteOrder.LITTLE_ENDIAN);

                        if (data.remaining() < 1)
                            return;
                        byte subscriptionMode = data.get();

                        if (subscriptionMode == 1 || subscriptionMode == 2) {
                            if (subscriptionMode == 1 && data.remaining() < 46)
                                return;
                            if (subscriptionMode == 2 && data.remaining() < 66)
                                return;

                            byte exchangeType = data.get();

                            byte[] tokenBytes = new byte[25];
                            data.get(tokenBytes);
                            String token = new String(tokenBytes).trim();

                            long sequenceNumber = data.getLong();
                            long exchangeTimestamp = data.getLong();

                            int rawLtp = data.getInt();
                            double ltp = rawLtp / 100.0;

                            String symbol = ScripMasterService.getSymbol(token);
                            if (symbol != null) {
                                engine.onPriceUpdate(symbol, ltp);
                            }
                            log.info("symbol {} and Price :{}", symbol, ltp);

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
                        // log.debug("onBinary msg received: {}", data);
                        parseMarketData(data);
                        webSocket.request(1);
                        return null;
                    }

                    @Override
                    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                        log.info("Angel One WebSocket Closed: {} / {}", statusCode, reason);
                        return null;
                    }

                    @Override
                    public void onError(WebSocket webSocket, Throwable error) {
                        log.error("Angel One WebSocket Error: {}", error.getMessage());
                    }
                }).join();

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
