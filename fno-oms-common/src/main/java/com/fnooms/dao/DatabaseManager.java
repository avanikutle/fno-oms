package com.fnooms.dao;

import com.fnooms.util.AppConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton HikariCP connection pool manager.
 * Auto-runs schema.sql on first startup to create all tables.
 */
public class DatabaseManager {

    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);
    private static volatile DatabaseManager instance;
    private final HikariDataSource dataSource;

    private DatabaseManager() {
        AppConfig cfg = AppConfig.getInstance();

        // Explicitly register the PostgreSQL JDBC driver.
        // Required in webapp classloaders where ServiceLoader-based auto-discovery
        // may not fire before HikariCP calls DriverManager.getDriver().
        try {
            Class.forName("org.postgresql.Driver");
            log.info("PostgreSQL JDBC driver registered");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("PostgreSQL JDBC driver not found in classpath", e);
        }

        HikariConfig hikariCfg = new HikariConfig();
        hikariCfg.setJdbcUrl(cfg.getDbUrl());
        hikariCfg.setUsername(cfg.getDbUsername());
        hikariCfg.setPassword(cfg.getDbPassword());
        hikariCfg.setMaximumPoolSize(cfg.getDbMaxPool());
        hikariCfg.setMinimumIdle(cfg.getDbMinIdle());
        hikariCfg.setConnectionTimeout(30_000);
        hikariCfg.setIdleTimeout(600_000);
        hikariCfg.setMaxLifetime(1_800_000);
        hikariCfg.setPoolName("fno-oms-pool");
        hikariCfg.setConnectionTestQuery("SELECT 1");
        hikariCfg.setDriverClassName("org.postgresql.Driver");

        // PostgreSQL-specific optimisations
        hikariCfg.addDataSourceProperty("cachePrepStmts",          "true");
        hikariCfg.addDataSourceProperty("prepStmtCacheSize",        "250");
        hikariCfg.addDataSourceProperty("prepStmtCacheSqlLimit",    "2048");
        hikariCfg.addDataSourceProperty("useServerPrepStmts",       "true");

        this.dataSource = new HikariDataSource(hikariCfg);
        log.info("HikariCP pool initialised → {}", cfg.getDbUrl());

        runSchema();
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) instance = new DatabaseManager();
            }
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            log.info("HikariCP pool closed.");
        }
    }

    /** Run schema.sql from classpath based on a flag in algo_key_value. */
    private void runSchema() {
        boolean shouldRun = true;
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery("SELECT key_value FROM algo_key_value WHERE key_name = 'schema.run'")) {
            if (rs.next()) {
                String val = rs.getString(1);
                if ("false".equalsIgnoreCase(val)) {
                    shouldRun = false;
                }
            }
        } catch (SQLException e) {
            // Table might not exist yet, proceed to run schema
        }

        if (!shouldRun) {
            log.info("Skipping schema.sql execution based on algo_key_value flag (schema.run = false).");
            return;
        }

        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("db/schema.sql")) {
            if (is == null) {
                log.warn("schema.sql not found on classpath — skipping auto-migration");
                return;
            }
            String sql = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining("\n"));

            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
                log.info("Schema migration complete.");
                
                // Set the flag to prevent running again
                String updater = AppConfig.getInstance().getAppUpdater();
                stmt.executeUpdate("INSERT INTO algo_key_value (key_name, key_value, updated_by) VALUES ('schema.run', 'false', '" + updater + "') ON CONFLICT(key_name) DO UPDATE SET key_value='false'");
            }
        } catch (Exception e) {
            log.error("Failed to run schema.sql: {}", e.getMessage(), e);
        }
    }
}
