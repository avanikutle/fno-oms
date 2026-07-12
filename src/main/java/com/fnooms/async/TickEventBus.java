package com.fnooms.async;

import com.fnooms.async.event.TickEvent;
import com.fnooms.dao.TickDAO;
import com.fnooms.util.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Singleton bus for persisting quote tick events to the TimescaleDB hypertable.
 *
 * <p>Unlike other buses, this one batches ticks and does a single bulk INSERT
 * every {@code tick.batch.flush.ms} milliseconds (default 500ms) for high
 * throughput into TimescaleDB. Up to {@code tick.batch.size} ticks per batch.
 */
public class TickEventBus extends AbstractEventBus<TickEvent> {

    private static final Logger log = LoggerFactory.getLogger(TickEventBus.class);
    private static volatile TickEventBus instance;

    private final TickDAO tickDAO   = new TickDAO();
    private final int     batchSize;
    private final long    flushMs;

    private TickEventBus() {
        super(AppConfig.getInstance().getTickQueueCapacity());
        this.batchSize = AppConfig.getInstance().getTickBatchSize();
        this.flushMs   = AppConfig.getInstance().getTickBatchFlushMs();
    }

    public static TickEventBus getInstance() {
        if (instance == null) {
            synchronized (TickEventBus.class) {
                if (instance == null) instance = new TickEventBus();
            }
        }
        return instance;
    }

    /**
     * Override start to run a batch-oriented drain loop instead of the
     * one-by-one parent loop.
     */
    @Override
    public void start() {
        Thread batchThread = new Thread(this::batchDrainLoop, "bus-writer-ticks");
        batchThread.setDaemon(true);
        batchThread.start();
        log.info("[ticks] Tick event bus started (capacity={}, batchSize={}, flushMs={})",
                getQueueCapacity(), batchSize, flushMs);
    }

    private void batchDrainLoop() {
        List<TickEvent> batch = new ArrayList<>(batchSize);
        while (true) {
            try {
                // Wait up to flushMs for first event, then drain up to batchSize
                TickEvent first = queue.poll(flushMs, TimeUnit.MILLISECONDS);
                if (first != null) {
                    batch.add(first);
                    queue.drainTo(batch, batchSize - 1);
                }
                if (!batch.isEmpty()) {
                    tickDAO.batchInsert(batch);
                    log.debug("[ticks] Batch inserted {} ticks", batch.size());
                    batch.clear();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                // Final flush
                if (!batch.isEmpty()) {
                    try { tickDAO.batchInsert(batch); } catch (Exception ex) {
                        log.error("[ticks] Final flush failed", ex);
                    }
                }
                break;
            } catch (Exception e) {
                log.error("[ticks] Batch insert error", e);
                batch.clear(); // discard on error to avoid infinite retry
            }
        }
    }

    @Override
    protected void processEvent(TickEvent event) {
        // Not used — batchDrainLoop handles processing directly
    }

    @Override
    protected String getBusName() { return "ticks"; }
}
