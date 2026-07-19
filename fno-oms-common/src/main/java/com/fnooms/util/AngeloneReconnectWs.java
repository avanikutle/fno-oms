package com.fnooms.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;

public class AngeloneReconnectWs {
    // Angel One Smart Stream WebSocket URL
    private static final String WS_URL = "wss://smartapisocket.angelone.in/smart-stream";

    public static void main(String[] args) throws Exception {
        int count = 0;
        while (true) {
            try {
                System.out.println("Iteration count " + count++);
                connectAndListen();
            } catch (Exception e) {
                System.err.println("Reconnect after error: " + e.getMessage());
            }
            Thread.sleep(5000);
        }
    }

    private static void connectAndListen() {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        client.newWebSocketBuilder()
                // Required headers for Angel One Smart Stream 2.0
                .header("Authorization", "Bearer " + getJwtToken())
                .header("x-api-key", getApiKey())
                .header("x-client-code", getClientCode())
                .header("x-feed-token", getFeedToken())
                .buildAsync(URI.create(WS_URL), new WebSocket.Listener() {
                    @Override
                    public void onOpen(WebSocket webSocket) {
                        System.out.println("Connected to Angel One");
                        webSocket.request(1);

                        // Subscribe payload for Angel One
                        // action: 1 (Subscribe), mode: 1 (LTP), exchangeType: 1 (NSE), 2 (NFO)
                        String subscribeMsg = """
                                {
                                  "correlationID": "abce12345",
                                  "action": 1,
                                  "params": {
                                    "mode": 2,
                                    "tokenList": [
                                      {
                                        "exchangeType": 2,
                                        "tokens": ["51378"]
                                      }
                                    ]
                                  }
                                }
                                """;

                        // "token": "117",
                        // "symbol": "GOLDMCOM",
                        // "name": "GOLDM",
                        // "expiry": "",
                        // "strike": "0.000000",
                        // "lotsize": "100",
                        // "instrumenttype": "COMDTY",
                        // "exch_seg": "MCX",
                        // "tick_size": "100.000000",
                        // "freeze_qty": "99999999"
                        webSocket.sendText(subscribeMsg, true);
                    }

                    // Define a basic mapping to print symbol names instead of just tokens
                    private static final Map<String, String> tokenMap = new HashMap<>();
                    static {
                        // "token": "51378", "symbol": "NIFTY14JUL2624150PE",
                        tokenMap.put("51378", "NIFTY14JUL2624150PE");
                        tokenMap.put("1594", "INFY");
                        tokenMap.put("2885", "RELIANCE");
                        tokenMap.put("26000", "NIFTY 50");
                        tokenMap.put("11536", "TCS");
                        tokenMap.put("63948", "NIFTY26JUL24200PE");

                        // Example Placeholders for Nifty Options
                        tokenMap.put("51379", "NIFTY 24200 CE");
                        tokenMap.put("41000", "NIFTY 24200 PE");
                    }

                    private void parseMarketData(ByteBuffer data) {
                        // Angel One Smart Stream uses LITTLE_ENDIAN
                        data.order(ByteOrder.LITTLE_ENDIAN);

                        if (data.remaining() < 1)
                            return;

                        byte subscriptionMode = data.get();

                        // Mode 1: LTP, Mode 2: Quote
                        if (subscriptionMode == 1 || subscriptionMode == 2) {
                            // Check we have enough bytes for the fields we read
                            if (subscriptionMode == 1 && data.remaining() < 46) {
                                System.out.println("Mode 1 but not enough bytes: " + data.remaining());
                                return;
                            }
                            if (subscriptionMode == 2 && data.remaining() < 66) {
                                System.out.println("Mode 2 but not enough bytes: " + data.remaining());
                                return;
                            }

                            byte exchangeType = data.get();

                            // Token is 25 bytes string padded with null characters
                            byte[] tokenBytes = new byte[25];
                            data.get(tokenBytes);
                            String token = new String(tokenBytes).trim();

                            long sequenceNumber = data.getLong();
                            long exchangeTimestamp = data.getLong();

                            int rawLtp = data.getInt();
                            double ltp = rawLtp / 100.0;

                            long ltq = 0;
                            long volume = 0;

                            if (subscriptionMode == 2) {
                                ltq = data.getLong();
                                int rawAtp = data.getInt();
                                volume = data.getLong();
                            }

                            String symbol = tokenMap.getOrDefault(token, "UNKNOWN_TOKEN_" + token);

                            // if (subscriptionMode == 2) {
                            // System.out.printf(
                            // "Symbol: %-15s | Token: %-6s | LTP: %.2f | LTQ: %d | Vol: %d | Tick ID: %d |
                            // Timestamp: %d%n",
                            // symbol, token, ltp, ltq, volume, sequenceNumber, exchangeTimestamp);
                            // } else {
                            System.out.printf(
                                    "Symbol: %-15s | Token: %-6s |  Tick ID: %d | Timestamp: %d |Live Price: %.2f %n",
                                    symbol, token, sequenceNumber, exchangeTimestamp, ltp);
                            // }

                            if (ltp > 80.35) {
                                // System.out.printf("zzzzzzzzzzzz Take trade %s %s %.2f%n", symbol, token,
                                // ltp);
                            }
                        }
                    }

                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        System.out.println(data);
                        webSocket.request(1);
                        return null;
                    }

                    @Override
                    public CompletionStage<?> onBinary(WebSocket webSocket, java.nio.ByteBuffer data, boolean last) {
                        // System.out.println("Binary message received: " + data.remaining() + "
                        // bytes");
                        // Let's print out the size to debug why mode 2 isn't streaming
                        System.out.print("Binary message received: " + data.remaining() + " bytes");
                        parseMarketData(data);
                        webSocket.request(1);
                        return null;
                    }

                    @Override
                    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                        System.out.println("Closed: " + statusCode + " / " + reason);
                        return null;
                    }

                    @Override
                    public void onError(WebSocket webSocket, Throwable error) {
                        System.err.println("Socket error: " + error.getMessage());
                    }
                }).join();

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Helper methods to get credentials
    private static String getJwtToken() {
        return CredsUtil.getJwtToken("angelone");
    }

    private static String getApiKey() {
        return CredsUtil.getApiKey("angelone");
    }

    private static String getClientCode() {
        return CredsUtil.getAngelOneClientCode("angelone");
    }

    private static String getFeedToken() {
        return CredsUtil.getAngelOneFeedToken("angelone");
    }
}
