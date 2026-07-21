package com.fnooms.algo;

import com.fnooms.broker.dto.OrderRequest;
import com.fnooms.broker.dto.OrderResponse;
import com.fnooms.service.OrderService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StrategyEngine {
    private static final Logger log = LoggerFactory.getLogger(StrategyEngine.class);

    private final OrderService orderService;
    private final com.fnooms.dao.AlgoKeyValueDAO kvDao;
    private final com.fnooms.dao.TradeStatusDAO tradeStatusDao;
    private final com.fnooms.dao.OrderDetailsDAO orderDetailsDao;
    private final Map<String, List<StrategyConfig>> configsBySymbol = new ConcurrentHashMap<>();
    private final Map<Long, TradeState> statesByStrategyId = new ConcurrentHashMap<>();
    private final Map<String, Double> lastPrices = new ConcurrentHashMap<>();

    public StrategyEngine(OrderService orderService, com.fnooms.dao.AlgoKeyValueDAO kvDao,
            com.fnooms.dao.TradeStatusDAO tradeStatusDao) {
        this.orderService = orderService;
        this.kvDao = kvDao;
        this.tradeStatusDao = tradeStatusDao;
        this.orderDetailsDao = new com.fnooms.dao.OrderDetailsDAO();
    }

    public void addConfig(StrategyConfig config) {
        configsBySymbol.computeIfAbsent(config.getSymbol(), k -> new ArrayList<>()).add(config);

        TradeState state = tradeStatusDao.loadLatestState(config.getStrategyId());
        statesByStrategyId.put(config.getStrategyId(), state);

        log.info("Loaded strategy config {} for symbol: {}. State -> Entered: {}, Exited: {}", config.getStrategyId(),
                config.getSymbol(), state.isEntered(), state.isExited());
    }

    public void removeConfig(Long strategyId) {
        // find config by strategy id and remove it
        for (List<StrategyConfig> list : configsBySymbol.values()) {
            list.removeIf(c -> c.getStrategyId().equals(strategyId));
        }
        statesByStrategyId.remove(strategyId);
        log.info("Removed strategy config for strategyId: {}", strategyId);
    }

    public TradeState getTradeState(Long strategyId) {
        return statesByStrategyId.get(strategyId);
    }

    public Map<String, Double> getLastPrices() {
        return lastPrices;
    }

    public void resetState(Long strategyId) {
        if (statesByStrategyId.containsKey(strategyId)) {
            statesByStrategyId.put(strategyId, new TradeState());
            log.info("Manually reset trade state for strategyId: {}", strategyId);
        }
    }

    public void onPriceUpdate(String symbol, double currentPrice) {
        lastPrices.put(symbol, currentPrice);
        log.info("Price Update: {} :{}", symbol, currentPrice);
        List<StrategyConfig> configs = configsBySymbol.get(symbol);
        if (configs == null || configs.isEmpty())
            return; // No strategy for this symbol

        for (StrategyConfig config : configs) {
            processConfigForPriceUpdate(config, symbol, currentPrice);
        }
    }

    private void processConfigForPriceUpdate(StrategyConfig config, String symbol, double currentPrice) {
        TradeState state = statesByStrategyId.get(config.getStrategyId());

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
                        int steps = (int) Math
                                .floor((currentPrice - state.getEntryPrice()) / config.getTrailingSlPoints());
                        // Place SL at entryPrice initially, then step it up by trailingSlPoints
                        newSl = state.getEntryPrice() + (steps - 1) * config.getTrailingSlPoints();
                    } else {
                        // Strict continuous trailing by the difference
                        newSl = currentPrice - config.getTrailingSlPoints();
                    }

                    if (newSl > state.getCurrentStopLoss()) {
                        state.setCurrentStopLoss(newSl);
                        log.info("Trailing SL for {} updated to {}", symbol, newSl);
                        tradeStatusDao.updateState(symbol, state, "Trailing SL updated to " + newSl);
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

                // Initial SL is strictly the configured stop loss until trailing kicks in
                state.setCurrentStopLoss(config.getStopLossPrice());

                String brokerType = kvDao.getValue("algo.orderBroker");
                if (brokerType == null)
                    brokerType = "MOCK"; // fallback
                tradeStatusDao.saveNewState(config.getStrategyId(), config.getSymbol(), config.getToken(), state,
                        "Entered Long Trade", brokerType);

                // Insert to order_details
                orderDetailsDao.insertEntry(config.getSymbol(), response.getBrokerOrderId(),
                        config.getTransactionType(), state.getEntryPrice(), brokerType);

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
                state.setExited(true); // Prevents further entries until reset or next day
                tradeStatusDao.updateState(config.getSymbol(), state, "Trade Exited");

                String brokerType = kvDao.getValue("algo.orderBroker");
                if (brokerType == null)
                    brokerType = "MOCK";
                // Update order_details
                orderDetailsDao.updateExit(config.getSymbol(), state.getEntryOrderId(), response.getBrokerOrderId(),
                        exitPrice, brokerType);

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
