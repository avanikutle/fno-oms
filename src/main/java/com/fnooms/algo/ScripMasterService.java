package com.fnooms.algo;

import com.fnooms.dao.AlgoKeyValueDAO;
import com.fnooms.dao.ScripMasterDAO;
import com.fnooms.util.AngelOneScripMasterFetcher;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ScripMasterService {
    private static final Logger log = LoggerFactory.getLogger(ScripMasterService.class);
    private static final String FILE_NAME = "angelone_scripmaster.json";
    
    // Tiny in-memory map ONLY for the 3-4 actively traded symbols
    private static final Map<String, String> symbolToTokenMap = new ConcurrentHashMap<>();
    private static final Map<String, String> tokenToSymbolMap = new ConcurrentHashMap<>();

    public static void loadScripMaster() {
        Path filePath = Paths.get(FILE_NAME);
        AlgoKeyValueDAO kvDao = new AlgoKeyValueDAO();
        ScripMasterDAO scripDao = new ScripMasterDAO();
        String todayDate = LocalDate.now().toString();
        
        String lastFetchDate = kvDao.getValue("scrip.master.date");
        
        if (!todayDate.equals(lastFetchDate) || !Files.exists(filePath)) {
            log.info("Scrip master is outdated or missing. Fetching and DB batch-inserting now for date: {}", todayDate);
            AngelOneScripMasterFetcher.fetchAndSaveScripMaster();
            
            if (Files.exists(filePath)) {
                try (Reader reader = new FileReader(filePath.toFile())) {
                    log.info("Parsing scrip master JSON for DB batch insert...");
                    Gson gson = new Gson();
                    JsonArray array = gson.fromJson(reader, JsonArray.class);
                    
                    List<JsonObject> list = new ArrayList<>();
                    for (JsonElement element : array) {
                        list.add(element.getAsJsonObject());
                    }
                    
                    scripDao.batchInsert(list);
                    kvDao.setValue("scrip.master.date", todayDate, "SYSTEM");
                } catch (Exception e) {
                    log.error("Error reading scrip master JSON for DB insert", e);
                }
            } else {
                log.error("Failed to fetch or find scrip master file!");
            }
        } else {
            log.info("Scrip master table is up to date (fetched on {}). Skipping download.", lastFetchDate);
        }
    }

    /**
     * Call this ONLY with the specific symbols you are actively trading (e.g., from strategy.properties).
     * It looks them up in the Postgres DB and caches them in a tiny ConcurrentHashMap for high-frequency access.
     */
    public static void initActiveTokens(List<String> activeSymbols) {
        ScripMasterDAO scripDao = new ScripMasterDAO();
        for (String symbol : activeSymbols) {
            String token = scripDao.getTokenBySymbol(symbol);
            if (token != null) {
                symbolToTokenMap.put(symbol, token);
                tokenToSymbolMap.put(token, symbol);
                log.info("Cached active symbol mapping: {} -> {}", symbol, token);
            } else {
                log.warn("Could not find token for active symbol in scrip_master DB: {}", symbol);
            }
        }
    }

    public static String getToken(String symbol) {
        return symbolToTokenMap.get(symbol);
    }
    
    public static String getSymbol(String token) {
        return tokenToSymbolMap.get(token);
    }
}
