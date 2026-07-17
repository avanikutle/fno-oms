package com.fnooms.util;

import com.fnooms.broker.dto.OrderRequest;
import com.fnooms.broker.mstock.MStockBrokerClient;

import java.io.InputStream;
import java.util.Properties;

import com.fnooms.util.CredsUtil;

/**
 * Utility class with a main method to quickly test and debug mStock API calls.
 */
public class MStockApiTester {

    public static void main(String[] args) {
        System.out.println("=== Starting MStock API Tester ===");

        String prefix = "mstock";
        String apiKey = CredsUtil.getApiKey(prefix);
        String jwtToken = CredsUtil.getJwtToken(prefix);

        if (apiKey == null || jwtToken == null) {
            System.err.println("Missing mstock.api_key or mstock.jwt_token in DB");
            return;
        }

        System.out.println("Credentials loaded successfully.");

        // Instantiate the SDK Client
        MStockBrokerClient client = new MStockBrokerClient(prefix);

        try {
            System.out.println("\n--- 1. Testing Order Book ---");
            var orders = client.getOrderBook();
            System.out.println("Order Book count: " + (orders != null ? orders.size() : 0));
            if (orders != null && !orders.isEmpty()) {
                orders.forEach(o -> System.out
                        .println(" - " + o.getSymbol() + " " + o.getTransactionType() + " " + o.getStatus()));
            }

            // System.out.println("\n--- 2. Testing Trade Book ---");
            // var trades = client.getTradeBook();
            // System.out.println("Trade Book count: " + (trades != null ? trades.size() :
            // 0));

            System.out.println("\n--- 3. Testing Portfolio (Holdings + Positions) ---");
            var holdings = client.getHoldings();
            var positions = client.getPositions();
            System.out.println("Holdings count: " + (holdings != null ? holdings.size() : 0));
            System.out.println("Positions count: " + (positions != null ? positions.size() : 0));

            System.out.println("\n--- 4. Testing Order Placement (Uncomment to execute) ---");
            /*
             * OrderRequest req = new OrderRequest()
             * .symbol("INFY-EQ")
             * .exchange("NSE")
             * .buy()
             * .market() // Or .limit(1200.0)
             * .quantity(1)
             * .product("CNC"); // CNC for delivery, MIS for intraday
             * 
             * var placedOrder = client.placeOrder(req);
             * System.out.println("Order placed! Broker Order ID: " +
             * placedOrder.getBrokerOrderId());
             */

            System.out.println("\n=== All Tests Completed ===");

        } catch (Exception e) {
            System.err.println("API Call failed!");
            e.printStackTrace();
        }
    }
}
