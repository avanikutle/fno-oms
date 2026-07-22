package com.fnooms.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrokerConfig {
    private static final Logger log = LoggerFactory.getLogger(BrokerConfig.class);
    private static final Properties properties = new Properties();

    static {
        try (InputStream input = BrokerConfig.class.getClassLoader().getResourceAsStream("broker-apis.properties")) {
            if (input == null) {
                log.error("Sorry, unable to find broker-apis.properties");
            } else {
                properties.load(input);
            }
        } catch (IOException ex) {
            log.error("Error loading broker-apis.properties", ex);
        }
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }
    
    public static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}
