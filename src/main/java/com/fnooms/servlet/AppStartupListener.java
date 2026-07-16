package com.fnooms.servlet;

import com.fnooms.async.AuditEventBus;
import com.fnooms.async.OrderEventBus;
import com.fnooms.async.TickEventBus;
import com.fnooms.dao.DatabaseManager;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lifecycle listener — boots all singletons on Tomcat startup,
 * gracefully drains all async queues on shutdown.
 */
public class AppStartupListener implements ServletContextListener {

    private static final Logger log = LoggerFactory.getLogger(AppStartupListener.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        log.info("========== FnO OMS Starting ==========");
        try {
            // 1. Initialise DB connection pool + run schema migration
            DatabaseManager.getInstance();
            log.info("✓ Database pool ready");

            // 2. Fetch today's scrip master
            com.fnooms.util.MStockScripMasterFetcher.fetchAndStoreScripMaster();
            log.info("✓ Scrip master loaded");

            // 3. Start async event bus writer threads
            OrderEventBus.getInstance().start();
            log.info("✓ OrderEventBus started");

            AuditEventBus.getInstance().start();
            log.info("✓ AuditEventBus started");

            TickEventBus.getInstance().start();
            log.info("✓ TickEventBus started");

            // 4. Start Algo Orchestrator in background
            com.fnooms.algo.AlgoManager.getInstance().start();
            log.info("✓ AlgoOrchestrator started");

            log.info("========== FnO OMS Ready ==========");
        } catch (Exception e) {
            log.error("FATAL: FnO OMS startup failed", e);
            throw new RuntimeException("Application startup failed", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        log.info("========== FnO OMS Shutting Down ==========");
        // Stop the Algo Orchestrator first so no new events are generated
        com.fnooms.algo.AlgoManager.getInstance().stop();

        // Drain all queues — flush pending DB writes before Tomcat stops
        OrderEventBus.getInstance().shutdownAndDrain();
        AuditEventBus.getInstance().shutdownAndDrain();
        TickEventBus.getInstance().shutdownAndDrain();
        DatabaseManager.getInstance().shutdown();
        log.info("========== FnO OMS Stopped ==========");
    }
}
