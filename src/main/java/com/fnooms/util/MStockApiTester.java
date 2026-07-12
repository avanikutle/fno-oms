package com.fnooms.util;

import com.fnooms.broker.dto.OrderRequest;
import com.fnooms.broker.mstock.MStockBrokerClient;
import com.fnooms.model.BrokerConfig;

import java.io.InputStream;
import java.util.Properties;

/**
 * Utility class with a main method to quickly test and debug mStock API calls.
 * Pulls credentials from src/main/resources/cred.properties.
 */
public class MStockApiTester {

    public static void main(String[] args) {
        System.out.println("=== Starting MStock API Tester ===");

        Properties props = new Properties();
        try (InputStream is = MStockApiTester.class.getClassLoader().getResourceAsStream("cred.properties")) {
            if (is == null) {
                System.err.println("Could not find cred.properties in resources folder!");
                return;
            }
            props.load(is);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        String apiKey = props.getProperty("mstock.api_key");
        String jwtToken = props.getProperty("mstock.jwt_token");

        if (apiKey == null || jwtToken == null) {
            System.err.println("Missing mstock.api_key or mstock.jwt_token in cred.properties");
            return;
        }

        System.out.println("Credentials loaded successfully.");

        // Create a dummy BrokerConfig
        BrokerConfig config = new BrokerConfig();
        config.setBrokerType("MSTOCK");
        config.setApiKey(apiKey);
        config.setAccessToken(jwtToken);
        config.setActive(true);

        // Instantiate the SDK Client
        MStockBrokerClient client = new MStockBrokerClient(config);

        try {
            System.out.println("\n--- 1. Testing Order Book ---");
            var orders = client.getOrderBook();
            System.out.println("Order Book count: " + (orders != null ? orders.size() : 0));
            if (orders != null && !orders.isEmpty()) {
                orders.forEach(o -> System.out.println(" - " + o.getSymbol() + " " + o.getTransactionType() + " " + o.getStatus()));
            }

            // System.out.println("\n--- 2. Testing Trade Book ---");
            // var trades = client.getTradeBook();
            // System.out.println("Trade Book count: " + (trades != null ? trades.size() : 0));

            System.out.println("\n--- 3. Testing Portfolio (Holdings + Positions) ---");
            var holdings = client.getHoldings();
            var positions = client.getPositions();
            System.out.println("Holdings count: " + (holdings != null ? holdings.size() : 0));
            System.out.println("Positions count: " + (positions != null ? positions.size() : 0));

            System.out.println("\n--- 4. Testing Order Placement (Uncomment to execute) ---");
            /*
            OrderRequest req = new OrderRequest()
                    .symbol("INFY-EQ")
                    .exchange("NSE")
                    .buy()
                    .market() // Or .limit(1200.0)
                    .quantity(1)
                    .product("CNC"); // CNC for delivery, MIS for intraday

            var placedOrder = client.placeOrder(req);
            System.out.println("Order placed! Broker Order ID: " + placedOrder.getBrokerOrderId());
            */

            System.out.println("\n=== All Tests Completed ===");

        } catch (Exception e) {
            System.err.println("API Call failed!");
            e.printStackTrace();
        }
    }
}
