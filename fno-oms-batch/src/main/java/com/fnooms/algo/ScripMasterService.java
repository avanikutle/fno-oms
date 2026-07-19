package com.fnooms.algo;

import java.util.List;
import java.util.Map;

public interface ScripMasterService {
    
    /** Gets the globally active instance. */
    static ScripMasterService getInstance() {
        return ScripMasterServiceProvider.getInstance();
    }

    void loadScripMaster();

    void initActiveTokens(List<String> activeSymbols);

    String getToken(String symbol);

    String getSymbol(String token);

    List<Map<String, String>> searchOptions(String query);
}
