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
    private final Map<String, StrategyConfig> configs = new ConcurrentHashMap<>();
    private final Map<String, TradeState> states = new ConcurrentHashMap<>();

    public StrategyEngine(OrderService orderService) {
        this.orderService = orderService;
    }

    public void addConfig(StrategyConfig config) {
        configs.put(config.getSymbol(), config);
        states.put(config.getSymbol(), new TradeState());
        log.info("Loaded strategy config for symbol: {}", config.getSymbol());
    }

    public void resetState(String symbol) {
        if (states.containsKey(symbol)) {
            states.put(symbol, new TradeState());
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
                executeEntry(config, state);
            }
        } else {
            // Check Exit Conditions (Assuming long trade for trailing logic right now, can
            // be expanded)
            boolean shouldExit = false;
            String exitReason = "";

            if (currentPrice >= config.getTargetPrice()) {
                shouldExit = true;
                exitReason = "TARGET REACHED";
            } else if (currentPrice <= state.getCurrentStopLoss()) {
                shouldExit = true;
                exitReason = "STOP LOSS HIT";
            } else if (config.getTrailingSlPoints() > 0) {
                // Trailing Stop Loss Logic (Assuming long position)
                // If price moves up favorably, move SL up.
                double newSl = currentPrice - config.getTrailingSlPoints();
                if (newSl > state.getCurrentStopLoss()) {
                    state.setCurrentStopLoss(newSl);
                    log.info("Trailing SL for {} updated to {}", symbol, newSl);
                }
            }

            if (shouldExit) {
                log.info("Exit condition ({}) met for {}. Price: {}", exitReason, symbol, currentPrice);
                executeExit(config, state);
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
                state.setCurrentStopLoss(config.getStopLossPrice());
                log.info("Successfully entered trade for {}. OrderId: {}", config.getSymbol(),
                        response.getBrokerOrderId());
            } else {
                log.error("Failed to place entry order for {}", config.getSymbol());
            }
        } catch (Exception e) {
            log.error("Error executing entry for {}: {}", config.getSymbol(), e.getMessage());
        }
    }

    private void executeExit(StrategyConfig config, TradeState state) {
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
