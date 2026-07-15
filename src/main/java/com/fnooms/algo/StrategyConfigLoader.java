package com.fnooms.algo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class StrategyConfigLoader {
    private static final Logger log = LoggerFactory.getLogger(StrategyConfigLoader.class);

    public static List<StrategyConfig> loadConfigs(String propertiesFileName) {
        List<StrategyConfig> configs = new ArrayList<>();
        Properties props = new Properties();

        try (InputStream in = StrategyConfigLoader.class.getClassLoader().getResourceAsStream(propertiesFileName)) {
            if (in == null) {
                log.error("Could not find {} in classpath.", propertiesFileName);
                return configs;
            }
            props.load(in);

            // Expecting keys like: strategy.symbols=INFY-EQ,TCS-EQ
            String symbolsStr = props.getProperty("strategy.symbols");
            if (symbolsStr == null || symbolsStr.isEmpty()) {
                log.warn("No 'strategy.symbols' found in properties.");
                return configs;
            }

            String[] symbolsArray = symbolsStr.split(",");
            List<String> activeSymbols = new ArrayList<>();
            for (String s : symbolsArray) {
                activeSymbols.add(s.trim());
            }
            
            // Hydrate the tiny in-memory map from the DB for ONLY these symbols
            ScripMasterService.initActiveTokens(activeSymbols);

            for (String symbol : activeSymbols) {
                StrategyConfig config = new StrategyConfig();
                config.setSymbol(symbol);
                
                // Get token from Scrip Master (which we just cached)
                String token = ScripMasterService.getToken(symbol);
                if (token == null) {
                    log.warn("Token not found for symbol: {}. Make sure scrip master is loaded and symbol is correct.", symbol);
                    continue; // Skip if we can't find token
                }
                config.setToken(token);

                String prefix = "strategy." + symbol + ".";
                config.setEntryPrice(Double.parseDouble(props.getProperty(prefix + "entryPrice")));
                config.setStopLossPrice(Double.parseDouble(props.getProperty(prefix + "stopLossPrice")));
                config.setTargetPrice(Double.parseDouble(props.getProperty(prefix + "targetPrice")));
                config.setTrailingSlPoints(Double.parseDouble(props.getProperty(prefix + "trailingSlPoints", "0.0")));
                config.setQuantity(Integer.parseInt(props.getProperty(prefix + "quantity")));
                config.setTransactionType(props.getProperty(prefix + "transactionType", "BUY").toUpperCase());
                config.setExchange(props.getProperty(prefix + "exchange", "NSE").toUpperCase());
                config.setProduct(props.getProperty(prefix + "product", "MIS").toUpperCase());
                
                String conditionStr = props.getProperty(prefix + "entryCondition", "GREATER_THAN_EQUAL");
                config.setEntryCondition(StrategyConfig.EntryCondition.valueOf(conditionStr));

                configs.add(config);
            }

        } catch (Exception e) {
            log.error("Failed to load strategy configs: {}", e.getMessage(), e);
        }

        return configs;
    }
}
