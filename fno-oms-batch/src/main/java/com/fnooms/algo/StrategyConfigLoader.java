package com.fnooms.algo;

import com.fnooms.dao.DatabaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StrategyConfigLoader {
    private static final Logger log = LoggerFactory.getLogger(StrategyConfigLoader.class);

    public static List<StrategyConfig> loadConfigs() {
        List<StrategyConfig> configs = new ArrayList<>();
        List<String> activeSymbols = new ArrayList<>();

        String sql = "SELECT id, scrip_name, exchange_id, name, entry_price, stop_loss, target_price, quantity, transaction_type, entry_condition, product, trailing_sl_points FROM strategies WHERE is_active = true";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                StrategyConfig config = new StrategyConfig();
                config.setStrategyId(rs.getLong("id"));
                String symbol = rs.getString("scrip_name");
                config.setSymbol(symbol);
                activeSymbols.add(symbol);

                config.setEntryPrice(rs.getDouble("entry_price"));
                config.setStopLossPrice(rs.getDouble("stop_loss"));
                config.setTargetPrice(rs.getDouble("target_price"));
                config.setQuantity(rs.getInt("quantity"));
                config.setTransactionType(rs.getString("transaction_type"));
                config.setExchange(rs.getString("exchange_id"));
                config.setProduct(rs.getString("product"));
                config.setTrailingSlPoints(rs.getDouble("trailing_sl_points"));

                String conditionStr = rs.getString("entry_condition");
                if (conditionStr != null && !conditionStr.isEmpty()) {
                    config.setEntryCondition(StrategyConfig.EntryCondition.valueOf(conditionStr));
                } else {
                    config.setEntryCondition(StrategyConfig.EntryCondition.GREATER_THAN_EQUAL);
                }

                configs.add(config);
            }

        } catch (Exception e) {
            log.error("Failed to load strategy configs from DB: {}", e.getMessage(), e);
            return configs;
        }

        // Hydrate the tiny in-memory map from the DB for ONLY these symbols
        ScripMasterService.getInstance().initActiveTokens(activeSymbols);

        // Populate tokens in configs
        for (StrategyConfig config : configs) {
            String token = ScripMasterService.getInstance().getToken(config.getSymbol());
            if (token == null) {
                log.warn("Token not found for symbol: {}. Make sure scrip master is loaded and symbol is correct.", config.getSymbol());
            } else {
                config.setToken(token);
            }
        }

        return configs;
    }
}
