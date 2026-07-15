package com.fnooms.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;
import java.util.concurrent.CompletionStage;

public class MstockReconnectWs {
    private static String ws_url = CredsUtil.getWsCreds();

    public static void main(String[] args) throws Exception {
        CredsUtil.getWsCreds();
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
                .buildAsync(URI.create(ws_url), new WebSocket.Listener() {
                    @Override
                    public void onOpen(WebSocket webSocket) {
                        System.out.println("Connected");
                        webSocket.request(1);

                        // Send login message to maintain session
                        webSocket.sendText("LOGIN:" + CredsUtil.getAccessToken(), true);
                        // "token": "51370", "symbol": "NIFTY14JUL2624000CE",
                        // "token": "51378", "symbol": "NIFTY14JUL2624150PE",
                        String subscribeMsg = """
                                {
                                  "a": "subscribe",
                                  "v": [51378]
                                }
                                """;

                        webSocket.sendText(subscribeMsg, true);
                        // send subscription payload here if required
                    }

                    // Define a basic mapping to print symbol names instead of just tokens
                    private static final java.util.Map<Integer, String> tokenMap = new java.util.HashMap<>();
                    static {
                        // instrument_token,exchange_token,tradingsymbol,name,last_price,expiry,strike,tick_size,lot_size,instrument_type,segment,exchange
                        // 11536,11536,TCS,TATA CONSULTANCY SERV LT,,,,0.1,1,EQ,EQ,NSE
                        // 62843,62843,FINNIFTY26JUL24200CE,FINNIFTY,2698.95,2026-07-28,24200,,60,CE,OPTIDX,NFO
                        // 63947,63947,NIFTY26JUL24200CE,NIFTY,261,2026-07-28,24200,,65,CE,OPTIDX,NFO
                        // 63948,63948,NIFTY26JUL24200PE,NIFTY,218.25,2026-07-28,24200,,65,PE,OPTIDX,NFO
                        // 40999,40999,NIFTY2681124200CE,NIFTY,368.7,2026-08-11,24200,,65,CE,OPTIDX,NFO
                        // 41000,41000,NIFTY2681124200PE,NIFTY,289.95,2026-08-11,24200,,65,PE,OPTIDX,NFO
                        //
                        tokenMap.put(1594, "INFY");
                        tokenMap.put(2885, "RELIANCE");
                        tokenMap.put(26000, "NIFTY 50");
                        tokenMap.put(11536, "TCS");
                        tokenMap.put(51370, "NIFTY14JUL2624000CE");
                        tokenMap.put(51378, "NIFTY14JUL2624150PE");
                        // Example Placeholders for Nifty Options (Tokens change every expiry!)
                        tokenMap.put(51379, "NIFTY 24200 CE");
                        tokenMap.put(41000, "NIFTY 24200 PE");
                    }

                    private void parseMarketData(ByteBuffer data) {
                        // Network protocols like m.Stock use BIG_ENDIAN byte order!
                        data.order(ByteOrder.BIG_ENDIAN);

                        // The buffer size usually tells us what type of packet it is.
                        // E.g., 52 bytes or 204 bytes are common for L1/L2 quotes.
                        int length = data.remaining();

                        if (length < 8)
                            return; // Ignore ping/pong or invalid tiny packets

                        // m.Stock usually sends a header (number of packets and size)
                        short packetCount = data.getShort();
                        short packetLength = data.getShort();

                        // Iterate over the number of packets inside this binary frame
                        for (int i = 0; i < packetCount; i++) {
                            if (data.remaining() < 4)
                                break;

                            // --- 1. Read Token (Instrument ID) ---
                            // Assuming Token is a 4-byte integer at the start of the packet data
                            int token = data.getInt();

                            // --- 2. Extract LTP (Last Traded Price) ---
                            // Typically, the price is an Integer located at a specific offset
                            // (e.g., let's assume it's right after the token).
                            // Note: Brokers often send price as (Actual Price * 100) to keep it an integer.
                            int rawLtp = data.getInt();
                            double ltp = rawLtp / 100.0;

                            String symbol = tokenMap.getOrDefault(token, "UNKNOWN_TOKEN_" + token);
                            System.out.printf("Symbol gg: %-15s | Token: %-6d | Live Price: %.2f%n", symbol,
                                    token, ltp);
                            if (ltp > 80.35) {
                                // System.out.printf("zzzzzzzzzzzz Take trade ", symbol, token,
                                // ltp);
                            }
                            // Advance the buffer position by the rest of the packet size
                            // to skip other fields (Volume, Bid, Ask, Open, High, Low)
                            int remainingPacketBytes = packetLength - 8; // We already read 8 bytes (token + LTP)
                            if (remainingPacketBytes > 0 && data.remaining() >= remainingPacketBytes) {
                                data.position(data.position() + remainingPacketBytes);
                            }
                        }
                    }

                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        System.out.println(data);
                        // System.out.println(data);
                        webSocket.request(1);
                        return null;
                    }

                    @Override
                    public CompletionStage<?> onBinary(WebSocket webSocket, java.nio.ByteBuffer data, boolean last) {
                        // System.out.println("Binary message received: " + data.remaining() + "
                        // bytes");
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
}