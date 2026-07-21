package com.fnooms.algo.benchmark;

import com.fnooms.broker.BrokerClient;
import com.fnooms.broker.BrokerClientFactory;
import com.fnooms.broker.dto.OrderResponse;
import com.fnooms.broker.dto.Position;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrokerBenchmarkMain {
    
    private static final Logger log = LoggerFactory.getLogger(BrokerBenchmarkMain.class);

    public static void main(String[] args) {
        // Disable noisy debug logging from clients so table is clear
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");
        
        System.out.println("=========================================================================");
        System.out.println("                 Broker API Latency Benchmark (Orders & Portfolio)       ");
        System.out.println("=========================================================================");
        System.out.println(String.format("%-15s | %-15s | %-12s | %-30s", "Broker", "Test Type", "Time (ms)", "Status"));
        System.out.println("-------------------------------------------------------------------------");

        String[] brokers = {"MSTOCK", "DHAN", "ANGELONE", "MOCK"};

        for (String brokerName : brokers) {
            try {
                BrokerClient client = BrokerClientFactory.getClientFor(brokerName);
                
                System.out.println("\n>>> [ " + brokerName + " ] <<<");

                // 1. Fetch Orders
                long startOrders = System.currentTimeMillis();
                try {
                    List<OrderResponse> orders = client.getOrderBook();
                    long timeOrders = System.currentTimeMillis() - startOrders;
                    printRow(brokerName, "Orders", timeOrders, "SUCCESS (" + (orders != null ? orders.size() : 0) + " items)");
                    
                    if (orders != null && !orders.isEmpty()) {
                        System.out.println("\n  --- Orders List ---");
                        System.out.println(String.format("  %-15s | %-20s | %-10s | %-8s | %-15s", "Order ID", "Symbol", "Txn Type", "Qty", "Status"));
                        System.out.println("  -------------------------------------------------------------------------------");
                        for (OrderResponse o : orders) {
                            System.out.println(String.format("  %-15s | %-20s | %-10s | %-8d | %-15s", 
                                o.getBrokerOrderId(), o.getSymbol(), o.getTransactionType(), o.getQuantity(), o.getStatus()));
                        }
                    } else {
                        System.out.println("\n  --- Orders List: EMPTY ---");
                    }

                } catch (Exception e) {
                    long timeOrders = System.currentTimeMillis() - startOrders;
                    printRow(brokerName, "Orders", timeOrders, "FAILED (" + e.getMessage() + ")");
                }

                // 2. Fetch Portfolio (Positions)
                long startPos = System.currentTimeMillis();
                try {
                    List<Position> positions = client.getPositions();
                    long timePos = System.currentTimeMillis() - startPos;
                    printRow(brokerName, "Portfolio", timePos, "SUCCESS (" + (positions != null ? positions.size() : 0) + " items)");
                    
                    if (positions != null && !positions.isEmpty()) {
                        System.out.println("\n  --- Portfolio Positions ---");
                        System.out.println(String.format("  %-20s | %-10s | %-8s | %-10s", "Symbol", "Product", "Net Qty", "PnL"));
                        System.out.println("  ---------------------------------------------------------");
                        for (Position p : positions) {
                            System.out.println(String.format("  %-20s | %-10s | %-8d | %-10s", 
                                p.getSymbol(), p.getProduct(), p.getQuantity(), (p.getPnl() != null ? p.getPnl().toString() : "N/A")));
                        }
                    } else {
                        System.out.println("\n  --- Portfolio Positions: EMPTY ---");
                    }

                } catch (Exception e) {
                    long timePos = System.currentTimeMillis() - startPos;
                    printRow(brokerName, "Portfolio", timePos, "FAILED (" + e.getMessage() + ")");
                }
            } catch (Exception e) {
                printRow(brokerName, "Initialization", 0, "FAILED (" + e.getMessage() + ")");
            }
            System.out.println("\n-------------------------------------------------------------------------");
        }
    }

    private static void printRow(String broker, String type, long time, String status) {
        String timeStr = time + " ms";
        System.out.println(String.format("%-15s | %-15s | %-12s | %-30s", broker, type, timeStr, status));
    }
}
