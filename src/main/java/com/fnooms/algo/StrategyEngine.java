package com.fnooms.algo;

import com.fnooms.broker.dto.OrderRequest;
import com.fnooms.broker.dto.OrderResponse;
import com.fnooms.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StrategyEngine {
    private static final Logger log = LoggerFactory.getLogger(StrategyEngine.class);

    private final OrderService orderService;
    private final com.fnooms.dao.AlgoKeyValueDAO kvDao;
    private final com.fnooms.dao.OrderDetailsDAO orderDetailsDao;
    private final Map<String, StrategyConfig> configs = new ConcurrentHashMap<>();
    private final Map<String, TradeState> states = new ConcurrentHashMap<>();

    public StrategyEngine(OrderService orderService, com.fnooms.dao.AlgoKeyValueDAO kvDao) {
        this.orderService = orderService;
        this.kvDao = kvDao;
        this.orderDetailsDao = new com.fnooms.dao.OrderDetailsDAO();
    }

    public void addConfig(StrategyConfig config) {
        configs.put(config.getSymbol(), config);
        
        TradeState state = new TradeState();
        String enteredStr = kvDao.getValue("state." + config.getSymbol() + ".entered");
        String exitedStr = kvDao.getValue("state." + config.getSymbol() + ".exited");
        String entryPriceStr = kvDao.getValue("state." + config.getSymbol() + ".entryPrice");
        String currentTargetStr = kvDao.getValue("state." + config.getSymbol() + ".currentTarget");
        
        if ("true".equalsIgnoreCase(enteredStr)) state.setEntered(true);
        if ("true".equalsIgnoreCase(exitedStr)) state.setExited(true);
        if (entryPriceStr != null) state.setEntryPrice(Double.parseDouble(entryPriceStr));
        if (currentTargetStr != null) state.setCurrentTarget(Double.parseDouble(currentTargetStr));
        
        states.put(config.getSymbol(), state);
        log.info("Loaded strategy config for symbol: {}. State -> Entered: {}, Exited: {}", config.getSymbol(), state.isEntered(), state.isExited());
    }

    public void resetState(String symbol) {
        if (states.containsKey(symbol)) {
            states.put(symbol, new TradeState());
            kvDao.setValue("state." + symbol + ".entered", "false", "SYSTEM");
            kvDao.setValue("state." + symbol + ".exited", "false", "SYSTEM");
            kvDao.setValue("state." + symbol + ".entryPrice", "0", "SYSTEM");
            kvDao.setValue("state." + symbol + ".currentTarget", "0", "SYSTEM");
            log.info("Manually reset trade state for symbol: {}", symbol);
        }
    }

    public void onPriceUpdate(String symbol, double currentPrice) {
        StrategyConfig config = configs.get(symbol);
        if (config == null)
            return; // No strategy for this symbol

        TradeState state = states.get(symbol);

        // If strategy has already finished (entered and exited), wait for manual reset.
        if (state.isExited()) {
            return;
        }

        if (!state.isEntered()) {
            // Check Entry Conditions
            boolean shouldEnter = false;
            if (config.getEntryCondition() == StrategyConfig.EntryCondition.GREATER_THAN_EQUAL) {
                if (currentPrice >= config.getEntryPrice())
                    shouldEnter = true;
            } else {
                if (currentPrice <= config.getEntryPrice())
                    shouldEnter = true;
            }

            if (shouldEnter) {
                log.info("Entry condition met for {}. Price: {} Threshold: {}", symbol, currentPrice,
                        config.getEntryPrice());
                // Save entry price before executing
                state.setEntryPrice(currentPrice);
                
                // Determine target price
                double target = config.getTargetPrice();
                if (currentPrice > target) {
                    target = currentPrice + 5.0; // Overridden target if entry is already higher
                }
                state.setCurrentTarget(target);
                
                executeEntry(config, state);
            }
        } else {
            // Check Exit Conditions (Assuming long trade for trailing logic right now, can
            // be expanded)
            boolean shouldExit = false;
            String exitReason = "";

            if (currentPrice >= state.getCurrentTarget()) {
                shouldExit = true;
                exitReason = "TARGET REACHED";
            } else if (currentPrice <= state.getCurrentStopLoss()) {
                shouldExit = true;
                exitReason = "STOP LOSS HIT";
            } else if (config.getTrailingSlPoints() > 0) {
                // Trailing Stop Loss Logic (Assuming long position)
                // 1. SL should only trail once price >= entryPrice + trailingSlPoints
                if (currentPrice >= state.getEntryPrice() + config.getTrailingSlPoints()) {
                    double newSl;
                    String trailMode = kvDao.getValue("algo.trailing.sl.mode"); // "step" or "continuous"
                    
                    if ("step".equalsIgnoreCase(trailMode)) {
                        int steps = (int) Math.floor((currentPrice - state.getEntryPrice()) / config.getTrailingSlPoints());
                        // Place SL at entryPrice initially, then step it up by trailingSlPoints
                        newSl = state.getEntryPrice() + (steps - 1) * config.getTrailingSlPoints();
                    } else {
                        // Strict continuous trailing by the difference
                        newSl = currentPrice - config.getTrailingSlPoints();
                    }
                    
                    if (newSl > state.getCurrentStopLoss()) {
                        state.setCurrentStopLoss(newSl);
                        log.info("Trailing SL for {} updated to {}", symbol, newSl);
                    }
                }
            }

            if (shouldExit) {
                log.info("Exit condition ({}) met for {}. Price: {}", exitReason, symbol, currentPrice);
                executeExit(config, state, currentPrice);
            }
        }
    }

    private void executeEntry(StrategyConfig config, TradeState state) {
        try {
            OrderRequest request = new OrderRequest()
                    .symbol(config.getSymbol())
                    .exchange(config.getExchange())
                    .product(config.getProduct())
                    .market()

                    .quantity(config.getQuantity());

            request.setTransactionType(config.getTransactionType());
            request.setAmo(true);
            OrderResponse response = orderService.placeOrder(request);
            if (response != null && response.getBrokerOrderId() != null) {
                state.setEntryOrderId(response.getBrokerOrderId());
                state.setEntered(true);
                kvDao.setValue("state." + config.getSymbol() + ".entered", "true", "SYSTEM");
                kvDao.setValue("state." + config.getSymbol() + ".entryPrice", String.valueOf(state.getEntryPrice()), "SYSTEM");
                kvDao.setValue("state." + config.getSymbol() + ".currentTarget", String.valueOf(state.getCurrentTarget()), "SYSTEM");
                
                // Initial SL is strictly the configured stop loss until trailing kicks in
                state.setCurrentStopLoss(config.getStopLossPrice());
                
                // Insert to order_details
                orderDetailsDao.insertEntry(config.getSymbol(), response.getBrokerOrderId(), config.getTransactionType(), state.getEntryPrice());
                
                log.info("Successfully entered trade for {}. OrderId: {}", config.getSymbol(),
                        response.getBrokerOrderId());
            } else {
                log.error("Failed to place entry order for {}", config.getSymbol());
            }
        } catch (Exception e) {
            log.error("Error executing entry for {}: {}", config.getSymbol(), e.getMessage());
        }
    }

    private void executeExit(StrategyConfig config, TradeState state, double exitPrice) {
        try {
            // To exit, we take the opposite transaction type.
            String exitSide = "BUY".equalsIgnoreCase(config.getTransactionType()) ? "SELL" : "BUY";

            OrderRequest request = new OrderRequest()
                    .symbol(config.getSymbol())
                    .exchange(config.getExchange())
                    .product(config.getProduct())
                    .market()
                    .quantity(config.getQuantity());

            request.setTransactionType(exitSide);

            OrderResponse response = orderService.placeOrder(request);
            if (response != null && response.getBrokerOrderId() != null) {
                state.setExitOrderId(response.getBrokerOrderId());
                state.setExited(true); // Prevents further entries
                kvDao.setValue("state." + config.getSymbol() + ".exited", "true", "SYSTEM");
                
                // Update order_details
                orderDetailsDao.updateExit(config.getSymbol(), state.getEntryOrderId(), response.getBrokerOrderId(), exitPrice);
                
                log.info("Successfully exited trade for {}. OrderId: {}", config.getSymbol(),
                        response.getBrokerOrderId());
            } else {
                log.error("Failed to place exit order for {}", config.getSymbol());
            }
        } catch (Exception e) {
            log.error("Error executing exit for {}: {}", config.getSymbol(), e.getMessage());
        }
    }
}
