package com.fnooms.async;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic bounded in-memory event bus backed by a {@link LinkedBlockingQueue}.
 *
 * <p>A single daemon writer thread drains the queue and calls
 * {@link #processEvent(T)} for each event. If the queue fills up,
 * {@link #publish(Object)} drops the event and logs a warning — the
 * order API call is NEVER blocked.
 *
 * <p>Call {@link #start()} on app startup and {@link #shutdownAndDrain()}
 * on app shutdown for graceful flushing.
 *
 * @param <T> The event type.
 */
public abstract class AbstractEventBus<T> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    protected final BlockingQueue<T> queue;
    private final Thread             writerThread;
    private final AtomicBoolean      running = new AtomicBoolean(false);
    private final int                capacity;

    protected AbstractEventBus(int capacity) {
        this.capacity = capacity;
        this.queue    = new LinkedBlockingQueue<>(capacity);
        this.writerThread = new Thread(this::drainLoop, "bus-writer-" + getBusName());
        this.writerThread.setDaemon(true);
    }

    /** Start the background writer daemon thread. */
    public void start() {
        if (running.compareAndSet(false, true)) {
            writerThread.start();
            log.info("[{}] Event bus started (capacity={})", getBusName(), capacity);
        }
    }

    /**
     * Publish an event. Never blocks — if queue is full, event is dropped.
     * Returns false if dropped.
     */
    public boolean publish(T event) {
        if (!running.get()) {
            log.warn("[{}] Bus not running, dropping event: {}", getBusName(), event);
            return false;
        }
        boolean offered = queue.offer(event);
        if (!offered) {
            log.warn("[{}] Queue FULL ({}/{}), dropping event!", getBusName(),
                    queue.size(), capacity);
        }
        return offered;
    }

    /**
     * Graceful shutdown: stop accepting new events, drain remaining queue,
     * then stop the writer thread. Waits up to 10 seconds.
     */
    public void shutdownAndDrain() {
        log.info("[{}] Shutting down — draining {} remaining events...",
                getBusName(), queue.size());
        running.set(false);
        writerThread.interrupt();
        // Drain remaining events synchronously on the calling thread
        T event;
        int drained = 0;
        while ((event = queue.poll()) != null) {
            try {
                processEvent(event);
                drained++;
            } catch (Exception e) {
                log.error("[{}] Error draining event on shutdown", getBusName(), e);
            }
        }
        log.info("[{}] Drained {} remaining events on shutdown.", getBusName(), drained);
    }

    /** The core drain loop running on the writer thread. */
    private void drainLoop() {
        log.debug("[{}] Writer thread started", getBusName());
        while (running.get() || !queue.isEmpty()) {
            try {
                // Poll with timeout so we can check 'running' periodically
                T event = queue.poll(200, TimeUnit.MILLISECONDS);
                if (event != null) {
                    processEvent(event);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("[{}] Error processing event", getBusName(), e);
            }
        }
        log.debug("[{}] Writer thread exited", getBusName());
    }

    /** Subclasses implement this to do the actual DB work. */
    protected abstract void processEvent(T event);

    /** Unique name for this bus — used in thread name and logs. */
    protected abstract String getBusName();

    public int getQueueSize()     { return queue.size(); }
    public int getQueueCapacity() { return capacity; }
    public boolean isRunning()    { return running.get(); }
}
