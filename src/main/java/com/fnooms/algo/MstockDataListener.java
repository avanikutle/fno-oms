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
    private volatile boolean running = true;
    private volatile WebSocket currentWebSocket;

    public MstockDataListener(StrategyEngine engine, List<StrategyConfig> configs) {
        this.engine = engine;
        this.configs = configs;
    }

    @Override
    public void stop() {
        running = false;
        if (currentWebSocket != null) {
            currentWebSocket.abort(); // or sendClose
        }
        log.info("MstockDataListener stopped.");
    }

    @Override
    public void addSubscription(String symbol) {
        String token = ScripMasterService.getToken(symbol);
        if (token != null && currentWebSocket != null) {
            String subscribeMsg = String.format(
                    "{\"correlationID\":\"sub_dyn\",\"action\":1,\"params\":{\"mode\":3,\"tokenList\":[{\"exchangeType\":2,\"tokens\":[\"%s\"]}]}}",
                    token);
            log.info("Dynamically subscribing on MStock: {}", subscribeMsg);
            currentWebSocket.sendText(subscribeMsg, true);
        } else {
            log.warn("Cannot subscribe to {}. Token: {}, WebSocket: {}", symbol, token,
                    currentWebSocket != null ? "connected" : "null");
        }
    }

    @Override
    public void start() {
        if (configs.isEmpty()) {
            log.error("MstockDataListener: No active strategies found to subscribe to.");
            return;
        }

        int count = 0;
        while (running) {
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
                        currentWebSocket = webSocket;
                        log.info("MStock WebSocket Connected to {}", ws_url);
                        printBanner(ws_url);
                        webSocket.request(1);

                        // Send login message to maintain session
                        webSocket.sendText("LOGIN:" + CredsUtil.getMStockJwtToken(), true);

                        // Build dynamic subscription payload based on configured tokens
                        List<String> tokens = configs.stream()
                                .map(StrategyConfig::getToken)
                                .filter(t -> t != null)
                                .collect(Collectors.toList());

                        // Add watchlist tokens
                        com.fnooms.dao.AlgoKeyValueDAO dao = new com.fnooms.dao.AlgoKeyValueDAO();
                        String watchlist = dao.getValue("algo.strategy.watchlist");
                        if (watchlist != null && !watchlist.isEmpty()) {
                            for (String sym : watchlist.split(",")) {
                                String t = ScripMasterService.getToken(sym.trim());
                                if (t != null && !tokens.contains(t)) {
                                    tokens.add(t);
                                }
                            }
                        }

                        if (!tokens.isEmpty()) {
                            String tokenArrayStr = tokens.stream().map(t -> "\"" + t + "\"")
                                    .collect(Collectors.joining(","));
                            String subscribeMsg = String.format(
                                    "{\"correlationID\":\"sub_init\",\"action\":1,\"params\":{\"mode\":3,\"tokenList\":[{\"exchangeType\":2,\"tokens\":[%s]}]}}",
                                    tokenArrayStr);
                            log.info("Subscribing on MStock: {}", subscribeMsg);
                            webSocket.sendText(subscribeMsg, true);
                        }
                    }

                    private void parseMarketData(ByteBuffer data) {
                        BinaryParserUtil.parseMStockMarketData(data, engine);
                    }

                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        log.info("Text msg received: {}", data);
                        webSocket.request(1);
                        return null;
                    }

                    @Override
                    public CompletionStage<?> onBinary(WebSocket webSocket, java.nio.ByteBuffer data, boolean last) {
                        log.debug("Binary msg received");
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
    private void printBanner(String url) {
        if (url != null && (url.contains("localhost") || url.contains("127.0.0.1"))) {
            System.out.println(
                "\n" +
                "========================================================================\n" +
                "  __  __   ____    _____   _  __     __  __    ____    _____   ______ \n" +
                " |  \\/  | / __ \\  / ____| | |/ /    |  \\/  |  / __ \\  |  __ \\ |  ____|\n" +
                " | \\  / || |  | || |      | ' /     | \\  / | | |  | | | |  | || |__   \n" +
                " | |\\/| || |  | || |      |  <      | |\\/| | | |  | | | |  | ||  __|  \n" +
                " | |  | || |__| || |____  | . \\     | |  | | | |__| | | |__| || |____ \n" +
                " |_|  |_| \\____/  \\_____| |_|\\_\\    |_|  |_|  \\____/  |_____/ |______|\n" +
                "                                                                      \n" +
                "             ::: CONNECTED TO LOCAL MOCK SERVER :::                   \n" +
                "========================================================================\n"
            );
        } else {
            System.out.println(
                "\n" +
                "========================================================================\n" +
                "  _        _____  __      __  ______     __  __   _____  _______  _  __\n" +
                " | |      |_   _| \\ \\    / / |  ____|   |  \\/  | / ____||__   __|| |/ /\n" +
                " | |        | |    \\ \\  / /  | |__      | \\  / || (___     | |   | ' / \n" +
                " | |        | |     \\ \\/ /   |  __|     | |\\/| | \\___ \\    | |   |  <  \n" +
                " | |____   _| |_     \\  /    | |____    | |  | | ____) |   | |   | . \\ \n" +
                " |______| |_____|     \\/     |______|   |_|  |_||_____/    |_|   |_|\\_\\\n" +
                "                                                                      \n" +
                "               ::: CONNECTED TO LIVE BROKER :::                       \n" +
                "========================================================================\n"
            );
        }
    }
}
