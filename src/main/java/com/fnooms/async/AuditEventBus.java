package com.fnooms.async;

import com.fnooms.async.event.AuditEvent;
import com.fnooms.dao.AuditDAO;
import com.fnooms.model.AuditLog;
import com.fnooms.util.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton bus for persisting audit log entries asynchronously.
 * Every broker API call publishes here — fire and forget.
 * Under extreme load, audit events may be dropped (queue full) — this is acceptable.
 */
public class AuditEventBus extends AbstractEventBus<AuditEvent> {

    private static final Logger log = LoggerFactory.getLogger(AuditEventBus.class);
    private static volatile AuditEventBus instance;
    private final AuditDAO auditDAO = new AuditDAO();

    private AuditEventBus() {
        super(AppConfig.getInstance().getAuditQueueCapacity());
    }

    public static AuditEventBus getInstance() {
        if (instance == null) {
            synchronized (AuditEventBus.class) {
                if (instance == null) instance = new AuditEventBus();
            }
        }
        return instance;
    }

    @Override
    protected void processEvent(AuditEvent event) {
        try {
            AuditLog log = new AuditLog(event.getAction(), event.getBrokerType(), event.getEndpoint());
            log.setRequest(truncate(event.getRequest(), 4000));
            log.setResponse(truncate(event.getResponse(), 4000));
            log.setStatusCode(event.getStatusCode());
            log.setLatencyMs(event.getLatencyMs());
            log.setError(truncate(event.getError(), 1000));
            auditDAO.insert(log);
        } catch (Exception e) {
            // Audit failures are non-critical — just log them
            AuditEventBus.log.warn("Failed to persist audit event: {}", e.getMessage());
        }
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }

    @Override
    protected String getBusName() { return "audit"; }
}
