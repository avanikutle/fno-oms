package com.fnooms.algo;

import com.fnooms.util.CredsUtil;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AngelOneDataListener implements MarketDataListener {
    private static final Logger log = LoggerFactory.getLogger(AngelOneDataListener.class);
    private static final String WS_URL = "wss://smartapisocket.angelone.in/smart-stream";

    private final StrategyEngine engine;
    private final List<StrategyConfig> configs;
    private final String brokerPrefix;
    private volatile WebSocket currentWebSocket;
    private final java.util.concurrent.ScheduledExecutorService pingScheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor();

    public AngelOneDataListener(StrategyEngine engine, List<StrategyConfig> configs, String brokerPrefix) {
        this.engine = engine;
        this.configs = configs;
        this.brokerPrefix = brokerPrefix;
        this.pingScheduler.scheduleAtFixedRate(() -> {
            if (currentWebSocket != null && !currentWebSocket.isInputClosed() && !currentWebSocket.isOutputClosed()) {
                try {
                    currentWebSocket.sendPing(ByteBuffer.wrap("ping".getBytes()));
                } catch (Exception e) {
                    log.warn("Failed to send ping to AngelOne WS: {}", e.getMessage());
                }
            }
        }, 20, 20, java.util.concurrent.TimeUnit.SECONDS);
    }

    @Override
    public void stop() {
        running = false;
        if (currentWebSocket != null) {
            currentWebSocket.abort();
        }
        pingScheduler.shutdownNow();
        log.info("AngelOneDataListener stopped.");
    }

    @Override
    public void addSubscription(String symbol) {
        String token = ScripMasterServiceProvider.getInstance("ANGELONE").getToken(symbol);
        if (token != null && currentWebSocket != null) {
            String tokenStr = "\"" + token + "\"";
            String subscribeMsg = String.format("""
                    {
                      "correlationID": "fnooms-algo",
                      "action": 1,
                      "params": {
                        "mode": 1,
                        "tokenList": [
                          {
                            "exchangeType": 1,
                            "tokens": [%s]
                          },
                          {
                            "exchangeType": 2,
                            "tokens": [%s]
                          },
                          {
                            "exchangeType": 3,
                            "tokens": [%s]
                          },
                          {
                            "exchangeType": 4,
                            "tokens": [%s]
                          },
                          {
                            "exchangeType": 5,
                            "tokens": [%s]
                          }
                        ]
                      }
                    }
                    """, tokenStr, tokenStr, tokenStr, tokenStr, tokenStr);
            log.info("Dynamically subscribing on Angel One: {}", subscribeMsg);
            currentWebSocket.sendText(subscribeMsg, true);
        }
    }

    private volatile boolean running = true;

    @Override
    public void start() {
        if (configs.isEmpty()) {
            log.warn("AngelOneDataListener: No active strategies found at startup. Will still connect for Watchlist.");
        }

        int count = 0;
        while (running) {
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
                .header("Authorization", "Bearer " + CredsUtil.getJwtToken(brokerPrefix))
                .header("x-api-key", CredsUtil.getApiKey(brokerPrefix))
                .header("x-client-code", CredsUtil.getAngelOneClientCode(brokerPrefix))
                .header("x-feed-token", CredsUtil.getAngelOneFeedToken(brokerPrefix))
                .buildAsync(URI.create(WS_URL), new WebSocket.Listener() {
                    @Override
                    public void onOpen(WebSocket webSocket) {
                        currentWebSocket = webSocket;
                        log.info("Angel One WebSocket Connected");
                        webSocket.request(1);

                        // Group tokens by exchangeType
                        java.util.Map<Integer, java.util.List<String>> tokensByExch = new java.util.HashMap<>();

                        for (StrategyConfig config : configs) {
                            String t = config.getToken();
                            if (t != null) {
                                int exchType = getExchangeType(config.getExchange());
                                tokensByExch.computeIfAbsent(exchType, k -> new java.util.ArrayList<>()).add(t);
                            }
                        }

                        // Add all cached options
                        for (com.google.gson.JsonObject opt : com.fnooms.dao.InstrumentDAO.getInstance().getAllCachedOptions()) {
                            String t = opt.get("token").getAsString();
                            String exchStr = opt.has("exchange") ? opt.get("exchange").getAsString() : "NFO";
                            int exchType = getExchangeType(exchStr);
                            
                            boolean found = tokensByExch.values().stream().anyMatch(list -> list.contains(t));
                            if (!found) {
                                tokensByExch.computeIfAbsent(exchType, k -> new java.util.ArrayList<>()).add(t);
                            }
                        }

                        // Add watchlist tokens (defaulting to NFO / exchangeType 2 if we don't know)
                        com.fnooms.dao.AlgoKeyValueDAO dao = new com.fnooms.dao.AlgoKeyValueDAO();
                        String watchlist = dao.getValue("algo.strategy.watchlist");
                        if (watchlist != null && !watchlist.isEmpty()) {
                            for (String sym : watchlist.split(",")) {
                                String t = ScripMasterServiceProvider.getInstance("ANGELONE").getToken(sym.trim());
                                if (t != null) {
                                    // ensure it's not already added somewhere
                                    boolean found = tokensByExch.values().stream().anyMatch(list -> list.contains(t));
                                    if (!found) {
                                        tokensByExch.computeIfAbsent(2, k -> new java.util.ArrayList<>()).add(t);
                                    }
                                }
                            }
                        }

                        if (!tokensByExch.isEmpty()) {
                            java.util.List<String> tokenListJsonParts = new java.util.ArrayList<>();
                            for (java.util.Map.Entry<Integer, java.util.List<String>> entry : tokensByExch.entrySet()) {
                                String tokenArrayStr = entry.getValue().stream().map(t -> "\"" + t + "\"")
                                        .collect(Collectors.joining(","));
                                tokenListJsonParts.add(String.format("""
                                          {
                                            "exchangeType": %d,
                                            "tokens": [%s]
                                          }""", entry.getKey(), tokenArrayStr));
                            }

                            String tokenListArrayStr = String.join(",\n", tokenListJsonParts);

                            String subscribeMsg = String.format("""
                                    {
                                      "correlationID": "fnooms-algo",
                                      "action": 1,
                                      "params": {
                                        "mode": 1,
                                        "tokenList": [
                                    %s
                                        ]
                                      }
                                    }
                                    """, tokenListArrayStr);

                            log.info("Subscribing on Angel One: {}", subscribeMsg);
                            webSocket.sendText(subscribeMsg, true);
                        }
                    }

                    private int getExchangeType(String exchangeStr) {
                        if (exchangeStr == null) return 2;
                        switch (exchangeStr.toUpperCase()) {
                            case "NSE": return 1;
                            case "NFO": return 2;
                            case "BSE": return 3;
                            case "BFO": return 4;
                            case "MCX": return 5;
                            default: return 2;
                        }
                    }

                    private void parseMarketData(ByteBuffer data) {
                        BinaryParserUtil.parseAngelOneMarketData(data, engine);
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
