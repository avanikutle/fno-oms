package com.fnooms.util;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.CompletionStage;

public class MstockReconnectWs {

    public static String WS_URL = null;

    public static String getWsCreds() {
        Properties props = new Properties();
        if (WS_URL != null) {
            return WS_URL;
        }
        try (InputStream is = MStockApiTester.class.getClassLoader().getResourceAsStream("cred.properties")) {
            if (is == null) {
                System.err.println("Could not find cred.properties in resources folder!");
            }
            props.load(is);
        } catch (Exception e) {
            e.printStackTrace();
        }

        String API_KEY = props.getProperty("mstock.api_key");
        String ACCESS_TOKEN = props.getProperty("mstock.jwt_token");
        WS_URL = "wss://ws.mstock.trade?API_KEY=" + API_KEY + "&ACCESS_TOKEN=" + ACCESS_TOKEN;
        System.out.println(WS_URL);
        return WS_URL;
    }

    public static void main(String[] args) throws Exception {
        MstockReconnectWs.getWsCreds();
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
                .buildAsync(URI.create(WS_URL), new WebSocket.Listener() {
                    @Override
                    public void onOpen(WebSocket webSocket) {
                        System.out.println("Connected");
                        webSocket.request(1);
                        String subscribeMsg = """
                                {
                                  "action": "subscribe",
                                  "symbols": ["NSE:sdffdsf NIFTY gg 50"]
                                }
                                """;

                        webSocket.sendText(subscribeMsg, true);
                        // send subscription payload here if required
                    }

                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        System.out.println(data);
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
}