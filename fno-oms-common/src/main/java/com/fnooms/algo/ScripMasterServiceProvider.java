package com.fnooms.algo;

import com.fnooms.dao.AlgoKeyValueDAO;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ScripMasterServiceProvider {
    private static final Map<String, ScripMasterService> instances = new ConcurrentHashMap<>();

    public static ScripMasterService getInstance(String brokerType) {
        if (brokerType == null) {
            AlgoKeyValueDAO dao = new AlgoKeyValueDAO();
            brokerType = dao.getValue("algo.orderBroker");
            if (brokerType == null) brokerType = "ANGELONE";
        }
        brokerType = brokerType.toUpperCase();
        
        return instances.computeIfAbsent(brokerType, k -> {
            if ("DHAN".equals(k)) {
                return new DhanScripMasterService();
            } else {
                return new AngelOneScripMasterService();
            }
        });
    }
    
    // Default fallback to orderBroker
    public static ScripMasterService getInstance() {
        return getInstance(null);
    }
}
