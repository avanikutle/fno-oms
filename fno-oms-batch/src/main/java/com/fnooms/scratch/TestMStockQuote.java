package com.fnooms.scratch;

import com.fnooms.broker.mstock.MStockBrokerClient;

public class TestMStockQuote {
    public static void main(String[] args) throws Exception {
        MStockBrokerClient client = new MStockBrokerClient("mstock");
        // Test various formats
        try {
            System.out.println("Testing Holdings to check if token is valid...");
            var holdings = client.getHoldings();
            System.out.println("Holdings count: " + (holdings != null ? holdings.size() : 0));
        } catch (Exception e) {
            System.out.println("Error for Holdings: " + e.getMessage());
        }
        
        String[] tests = {
            "NFO:NIFTY2680418900CE"
        };
        
        String[] endpoints = {
            "/market-quote/quotes",
            "/market/quotes",
            "/quotes",
            "/openapi/typea/market-quote/quotes"
        };
        
        for (String endpoint : endpoints) {
            for (String t : tests) {
                try {
                    System.out.println("Testing " + t + " with endpoint " + endpoint);
                    // use reflection or just change MStockMarketApi temporarily? No, I can't easily change it here. I'll just write an okhttp call.
                    com.fnooms.broker.mstock.MStockConfig config = new com.fnooms.broker.mstock.MStockConfig("mstock");
                    okhttp3.Request req = new okhttp3.Request.Builder()
                        .url(config.getBaseUrl().replace("/openapi/typeb", "") + (endpoint.startsWith("/") ? "" : "/") + endpoint + "?i=" + t)
                        .header("Authorization", config.getAuthorizationHeader())
                        .header("X-Mirae-Version", "1")
                        .build();
                    okhttp3.OkHttpClient http = new okhttp3.OkHttpClient();
                    try (okhttp3.Response resp = http.newCall(req).execute()) {
                        System.out.println("Result " + resp.code() + ": " + (resp.body() != null ? resp.body().string() : ""));
                    }
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }
        }
    }
}
