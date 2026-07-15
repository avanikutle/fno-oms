package com.fnooms.util;

import java.io.InputStream;
import java.util.Properties;

public class CredsUtil {
    public static String WS_URL = null;
    private static final Properties props = new Properties();

    public static String loadProperties() {

        if (WS_URL != null) {
            return WS_URL;
        }
        try (InputStream is = MStockApiTester.class.getClassLoader().getResourceAsStream("cred.properties")) {
            if (is == null) {
                System.err.println("Could not find cred.properties in resources folder!");
            }
            props.load(is);
        } catch (Exception e) {
            e.printStackTrace();
        }

        String API_KEY = props.getProperty("mstock.api_key");
        String ACCESS_TOKEN = props.getProperty("mstock.jwt_token");
        WS_URL = "wss://ws.mstock.trade?API_KEY=" + API_KEY + "&ACCESS_TOKEN=" + ACCESS_TOKEN;
        System.out.println(WS_URL);
        return WS_URL;
    }

    public static String getWsCreds() {
        if (props.isEmpty()) {
            loadProperties();
        }
        String API_KEY = props.getProperty("mstock.api_key");
        String ACCESS_TOKEN = props.getProperty("mstock.jwt_token");
        WS_URL = "wss://ws.mstock.trade?API_KEY=" + API_KEY + "&ACCESS_TOKEN=" + ACCESS_TOKEN;
        System.out.println("API_KEY==>>" + API_KEY);
        System.out.println("ACCESS_TOKEN==>>" + ACCESS_TOKEN);
        return WS_URL;
    }

    public static String getAccessToken() {
        if (props.isEmpty()) {
            loadProperties();
        }
        return props.getProperty("mstock.jwt_token");
    }

    public static String getAngelOneApiKey() {
        if (props.isEmpty()) {
            loadProperties();
        }
        return props.getProperty("angelone.api_key");
    }

    public static String getAngelOneJwtToken() {
        if (props.isEmpty()) {
            loadProperties();
        }
        return props.getProperty("angelone.jwt_token");
    }

    public static String getAngelOneClientCode() {
        if (props.isEmpty()) {
            loadProperties();
        }
        return props.getProperty("angelone.client_code");
    }

    public static String getAngelOneFeedToken() {
        if (props.isEmpty()) {
            loadProperties();
        }
        return props.getProperty("angelone.feed_token");
    }
}
