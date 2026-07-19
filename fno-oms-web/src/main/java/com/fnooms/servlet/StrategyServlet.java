package com.fnooms.servlet;

import com.fnooms.algo.*;
import com.fnooms.dao.AlgoKeyValueDAO;
import com.fnooms.dao.DatabaseManager;
import com.fnooms.util.JsonUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet("/api/strategies/*")
public class StrategyServlet extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(StrategyServlet.class);
    private final AlgoKeyValueDAO kvDao = new AlgoKeyValueDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        try {
            if ("/search".equals(path)) {
                String query = req.getParameter("q");
                List<Map<String, String>> results = ScripMasterService.getInstance().searchOptions(query);
                JsonUtil.writeJson(resp, 200, JsonUtil.success(results));
            } else if ("/watchlist".equals(path)) {
                String watchlist = kvDao.getValue("algo.strategy.watchlist");
                JsonUtil.writeJson(resp, 200, JsonUtil.success(watchlist == null ? "" : watchlist));
            } else {
                // Return all active strategies with their live state
                List<StrategyConfig> configs = StrategyConfigLoader.loadConfigs();
                com.fnooms.dao.TradeStatusDAO tradeStatusDao = new com.fnooms.dao.TradeStatusDAO();
                JsonArray resultArr = new JsonArray();
                for (StrategyConfig config : configs) {
                    TradeState state = tradeStatusDao.loadLatestState(config.getSymbol());
                    JsonObject obj = new JsonObject();
                    obj.add("config", JsonUtil.parseObject(JsonUtil.success(config).get("data").toString())); // Quick hack to serialize config to json object
                    
                    JsonObject stateObj = new JsonObject();
                    stateObj.addProperty("entered", state.isEntered());
                    stateObj.addProperty("exited", state.isExited());
                    stateObj.addProperty("entryPrice", state.getEntryPrice());
                    stateObj.addProperty("currentTarget", state.getCurrentTarget());
                    stateObj.addProperty("currentStopLoss", state.getCurrentStopLoss());
                    stateObj.addProperty("entryOrderId", state.getEntryOrderId());
                    stateObj.addProperty("exitOrderId", state.getExitOrderId());
                    
                    obj.add("state", stateObj);
                    resultArr.add(obj);
                }
                
                JsonUtil.writeJson(resp, 200, JsonUtil.success(resultArr));
            }
        } catch (Exception e) {
            log.error("Error in StrategyServlet GET", e);
            JsonUtil.writeJson(resp, 500, JsonUtil.error(e.getMessage()));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        try {
            String body = new String(req.getInputStream().readAllBytes());
            JsonObject json = JsonUtil.parseObject(body);

            if ("/watchlist".equals(path)) {
                String symbol = json.get("symbol").getAsString();
                String existing = kvDao.getValue("algo.strategy.watchlist");
                
                List<String> list = new ArrayList<>();
                if (existing != null && !existing.isEmpty()) {
                    for (String s : existing.split(",")) list.add(s.trim());
                }
                
                if (!list.contains(symbol)) {
                    list.add(symbol);
                    kvDao.setValue("algo.strategy.watchlist", String.join(",", list));
                    
                    MarketDataListener listener = AlgoManager.getInstance().getListener();
                    if (listener != null) {
                        listener.addSubscription(symbol);
                    }
                }
                JsonUtil.writeJson(resp, 200, JsonUtil.success("Added to watchlist"));
                
            } else if ("/add".equals(path)) {
                // Parse Strategy Config
                String symbol = json.get("symbol").getAsString();
                String exchange = json.has("exchange") ? json.get("exchange").getAsString() : "NFO";
                String name = json.has("name") ? json.get("name").getAsString() : symbol;
                double entryPrice = json.get("entry_price").getAsDouble();
                double targetPrice = json.get("target_price").getAsDouble();
                double stopLoss = json.get("stop_loss").getAsDouble();
                int quantity = json.get("quantity").getAsInt();
                String txType = json.has("transaction_type") ? json.get("transaction_type").getAsString() : "BUY";
                String condition = json.has("entry_condition") ? json.get("entry_condition").getAsString() : "GREATER_THAN_EQUAL";
                double trailingSlPoints = json.has("trailing_sl_points") ? json.get("trailing_sl_points").getAsDouble() : 0.0;
                String product = json.has("product") ? json.get("product").getAsString() : "MIS";
                
                // Insert into DB
                String sql = "INSERT INTO strategies (scrip_name, exchange_id, name, entry_price, target_price, stop_loss, trailing_sl_points, quantity, transaction_type, entry_condition, product, is_active) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                             "ON CONFLICT(scrip_name) DO UPDATE SET " +
                             "entry_price=EXCLUDED.entry_price, target_price=EXCLUDED.target_price, stop_loss=EXCLUDED.stop_loss, trailing_sl_points=EXCLUDED.trailing_sl_points, quantity=EXCLUDED.quantity;";
                             
                try (Connection conn = DatabaseManager.getInstance().getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, symbol);
                    pstmt.setString(2, exchange);
                    pstmt.setString(3, name);
                    pstmt.setDouble(4, entryPrice);
                    pstmt.setDouble(5, targetPrice);
                    pstmt.setDouble(6, stopLoss);
                    pstmt.setDouble(7, trailingSlPoints);
                    pstmt.setInt(8, quantity);
                    pstmt.setString(9, txType);
                    pstmt.setString(10, condition);
                    pstmt.setString(11, product);
                    pstmt.setBoolean(12, true); // is_active
                    pstmt.executeUpdate();
                }

                // Hydrate the config to memory
                List<String> list = new ArrayList<>(); list.add(symbol);
                ScripMasterService.getInstance().initActiveTokens(list);

                StrategyConfig config = new StrategyConfig();
                config.setSymbol(symbol);
                config.setExchange(exchange);
                config.setEntryPrice(entryPrice);
                config.setTargetPrice(targetPrice);
                config.setStopLossPrice(stopLoss);
                config.setQuantity(quantity);
                config.setTransactionType(txType);
                config.setEntryCondition(StrategyConfig.EntryCondition.valueOf(condition));
                config.setProduct(product);
                config.setToken(ScripMasterService.getInstance().getToken(symbol));
                
                StrategyEngine engine = AlgoManager.getInstance().getEngine();
                if (engine != null) {
                    engine.addConfig(config);
                    // reset state automatically for new config
                    kvDao.setValue("state." + symbol + ".entered", "false");
                    kvDao.setValue("state." + symbol + ".exited", "false");
                }
                
                MarketDataListener listener = AlgoManager.getInstance().getListener();
                if (listener != null) {
                    listener.addSubscription(symbol);
                }
                
                JsonUtil.writeJson(resp, 200, JsonUtil.success("Strategy added successfully"));
                
            } else if ("/reset".equals(path)) {
                String symbol = json.get("symbol").getAsString();
                kvDao.setValue("state." + symbol + ".entered", "false");
                kvDao.setValue("state." + symbol + ".exited", "false");
                JsonUtil.writeJson(resp, 200, JsonUtil.success("Strategy state reset for " + symbol));
            } else {
                JsonUtil.writeJson(resp, 404, JsonUtil.error("Not found"));
            }
        } catch (Exception e) {
            log.error("Error in StrategyServlet POST", e);
            JsonUtil.writeJson(resp, 500, JsonUtil.error(e.getMessage()));
        }
    }
}
