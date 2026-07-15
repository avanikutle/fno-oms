package com.fnooms.util;

import com.fnooms.dao.AlgoKeyValueDAO;

public class CredsUtil {
    private static final AlgoKeyValueDAO dao = new AlgoKeyValueDAO();
    public static String WS_URL = null;

    public static String getMStockApiKey() {
        return dao.getValue("mstock.api_key");
    }

    public static String getMStockJwtToken() {
        return dao.getValue("mstock.jwt_token");
    }

    public static String getWsCreds() {
        String API_KEY = getMStockApiKey();
        String ACCESS_TOKEN = getMStockJwtToken();
        WS_URL = "wss://ws.mstock.trade?API_KEY=" + API_KEY + "&ACCESS_TOKEN=" + ACCESS_TOKEN;
        return WS_URL;
    }

    public static String getAngelOneApiKey() {
        return dao.getValue("angelone.api_key");
    }

    public static String getAngelOneJwtToken() {
        return dao.getValue("angelone.jwt_token");
    }

    public static String getAngelOneClientCode() {
        return dao.getValue("angelone.client_code");
    }

    public static String getAngelOneFeedToken() {
        return dao.getValue("angelone.feed_token");
    }
}
