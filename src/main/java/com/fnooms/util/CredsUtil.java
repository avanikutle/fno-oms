package com.fnooms.util;

import com.fnooms.dao.AlgoKeyValueDAO;

public class CredsUtil {
    private static final AlgoKeyValueDAO dao = new AlgoKeyValueDAO();
    public static String WS_URL = null;

    public static String getApiKey(String prefix) {
        return dao.getValue(prefix + ".api_key");
    }

    public static String getJwtToken(String prefix) {
        return dao.getValue(prefix + ".jwt_token");
    }

    public static String getWsCreds(String prefix) {
        // Check if the mock WebSocket is explicitly enabled
        String isMockEnabled = dao.getValue("mock." + prefix + ".ws.url.enabled");
        if ("true".equalsIgnoreCase(isMockEnabled)) {
            String customUrl = dao.getValue("mock." + prefix + ".ws.url");
            if (customUrl != null && !customUrl.trim().isEmpty()) {
                return customUrl;
            }
        }

        String API_KEY = getApiKey(prefix);
        String ACCESS_TOKEN = getJwtToken(prefix);
        WS_URL = "wss://ws.mstock.trade?API_KEY=" + API_KEY + "&ACCESS_TOKEN=" + ACCESS_TOKEN;
        return WS_URL;
    }

    public static String getAngelOneClientCode(String prefix) {
        return dao.getValue(prefix + ".client_code");
    }

    public static String getAngelOneFeedToken(String prefix) {
        return dao.getValue(prefix + ".feed_token");
    }
}
