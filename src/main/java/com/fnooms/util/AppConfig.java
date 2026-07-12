package com.fnooms.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Singleton configuration loader.
 * Reads application.properties from classpath on first access.
 */
public class AppConfig {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);
    private static final String CONFIG_FILE = "application.properties";
    private static volatile AppConfig instance;
    private final Properties props = new Properties();

    private AppConfig() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (is == null) {
                throw new IllegalStateException("Cannot find " + CONFIG_FILE + " on classpath");
            }
            props.load(is);
            log.info("AppConfig loaded from {}", CONFIG_FILE);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load " + CONFIG_FILE, e);
        }
    }

    public static AppConfig getInstance() {
        if (instance == null) {
            synchronized (AppConfig.class) {
                if (instance == null) {
                    instance = new AppConfig();
                }
            }
        }
        return instance;
    }

    public String get(String key) {
        return props.getProperty(key);
    }

    public String get(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(props.getProperty(key, String.valueOf(defaultValue)).trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid integer for key '{}', using default {}", key, defaultValue);
            return defaultValue;
        }
    }

    public long getLong(String key, long defaultValue) {
        try {
            return Long.parseLong(props.getProperty(key, String.valueOf(defaultValue)).trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid long for key '{}', using default {}", key, defaultValue);
            return defaultValue;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String val = props.getProperty(key);
        return val != null ? Boolean.parseBoolean(val.trim()) : defaultValue;
    }

    // ---- Convenience accessors ----

    public String getDbUrl()       { return get("db.url"); }
    public String getDbUsername()  { return get("db.username"); }
    public String getDbPassword()  { return get("db.password"); }
    public int    getDbMaxPool()   { return getInt("db.pool.maxSize", 10); }
    public int    getDbMinIdle()   { return getInt("db.pool.minIdle", 2); }

    public int  getOrderQueueCapacity() { return getInt("queue.order.capacity", 10000); }
    public int  getAuditQueueCapacity() { return getInt("queue.audit.capacity", 50000); }
    public int  getTickQueueCapacity()  { return getInt("queue.tick.capacity", 100000); }

    public int  getTickBatchSize()    { return getInt("tick.batch.size", 500); }
    public long getTickBatchFlushMs() { return getLong("tick.batch.flush.ms", 500L); }

    public String getMStockBaseUrl()    { return get("mstock.base.url", "https://api.mstock.trade"); }
    public String getMStockApiVersion() { return get("mstock.api.version", "1"); }
}
