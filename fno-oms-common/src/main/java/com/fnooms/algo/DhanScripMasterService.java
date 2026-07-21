package com.fnooms.algo;

import com.fnooms.dao.DatabaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DhanScripMasterService implements ScripMasterService {
    private static final Logger log = LoggerFactory.getLogger(DhanScripMasterService.class);
    
    private final Map<String, String> symbolToTokenMap = new ConcurrentHashMap<>();
    private final Map<String, String> tokenToSymbolMap = new ConcurrentHashMap<>();

    @Override
    public void loadScripMaster() {
        com.fnooms.util.DhanScripMasterFetcher.fetchAndStoreScripMaster();
    }

    @Override
    public void initActiveTokens(List<String> activeSymbols) {
        for (String symbol : activeSymbols) {
            String token = getDhanToken(symbol);
            if (token != null) {
                symbolToTokenMap.put(symbol, token);
                tokenToSymbolMap.put(token, symbol);
                log.info("Cached active symbol mapping for DHAN: {} -> {}", symbol, token);
            } else {
                log.warn("Could not find DHAN token for active symbol: {}", symbol);
            }
        }
    }

    private String getDhanToken(String symbol) {
        String sql = "SELECT SEM_SMST_SECURITY_ID FROM dhan_scrip_master WHERE SEM_TRADING_SYMBOL = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, symbol);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("SEM_SMST_SECURITY_ID");
                }
            }
        } catch (Exception e) {
            log.error("Error looking up Dhan token for {}: {}", symbol, e.getMessage());
        }
        return null;
    }

    @Override
    public String getToken(String symbol) {
        return symbolToTokenMap.get(symbol);
    }

    @Override
    public String getSymbol(String token) {
        return tokenToSymbolMap.get(token);
    }

    @Override
    public List<Map<String, String>> searchOptions(String query) {
        List<Map<String, String>> results = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            return results;
        }
        String sql = "SELECT SEM_TRADING_SYMBOL, SEM_SMST_SECURITY_ID FROM dhan_scrip_master WHERE SEM_TRADING_SYMBOL ILIKE ? LIMIT 50";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "%" + query.trim() + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, String> map = new ConcurrentHashMap<>();
                    map.put("symbol", rs.getString("SEM_TRADING_SYMBOL"));
                    map.put("token", rs.getString("SEM_SMST_SECURITY_ID"));
                    results.add(map);
                }
            }
        } catch (Exception e) {
            log.error("Error searching DHAN options: ", e);
        }
        return results;
    }
}
