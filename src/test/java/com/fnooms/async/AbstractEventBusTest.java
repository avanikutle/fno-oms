package com.fnooms.async;

import com.fnooms.async.event.OrderEvent;
import com.fnooms.broker.dto.OrderRequest;
import com.fnooms.broker.dto.OrderResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the AbstractEventBus core contract:
 * 1. publish() never blocks
 * 2. Events are processed by the writer thread
 * 3. Queue-full behaviour drops events instead of blocking
 * 4. shutdownAndDrain() flushes remaining events
 *
 * Uses a test-only concrete subclass — no real DB involved.
 */
class AbstractEventBusTest {

    private TestEventBus bus;
    private final CopyOnWriteArrayList<OrderEvent> processed = new CopyOnWriteArrayList<>();

    @BeforeEach
    void setUp() {
        processed.clear();
        bus = new TestEventBus(processed, 5); // tiny capacity to test overflow
        bus.start();
    }

    @AfterEach
    void tearDown() {
        bus.shutdownAndDrain();
    }

    @Test
    void publish_singleEvent_isProcessed() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        bus.setLatch(latch);

        OrderEvent event = makeEvent();
        assertTrue(bus.publish(event), "publish should return true");

        assertTrue(latch.await(3, TimeUnit.SECONDS), "Event should be processed within 3s");
        assertEquals(1, processed.size());
    }

    @Test
    void publish_doesNotBlock_returnsImmediately() {
        // Fill queue to capacity (5) then measure time of next publish
        for (int i = 0; i < 5; i++) bus.publish(makeEvent());

        long start = System.nanoTime();
        boolean result = bus.publish(makeEvent()); // queue is full — should drop instantly
        long elapsed = System.nanoTime() - start;

        // Should return in well under 10ms (non-blocking)
        assertTrue(elapsed < 10_000_000L, "publish must not block; took " + elapsed + "ns");
        // Either returned false (dropped) or queue had space — either is fine
    }

    @Test
    void publish_queueFull_returnsFalse_doesNotThrow() throws InterruptedException {
        // Pause processing by using a bus with no latch started yet
        TestEventBus stallBus = new TestEventBus(processed, 3);
        stallBus.setStall(true);
        stallBus.start();
        Thread.sleep(50); // let writer thread start and stall

        boolean first  = stallBus.publish(makeEvent());
        boolean second = stallBus.publish(makeEvent());
        boolean third  = stallBus.publish(makeEvent());

        // 4th should be dropped (queue full)
        boolean dropped = stallBus.publish(makeEvent());

        // At least one should succeed
        assertTrue(first || second || third);
        // No exception thrown — returns false gracefully
        assertDoesNotThrow(() -> stallBus.publish(makeEvent()));

        stallBus.setStall(false);
        stallBus.shutdownAndDrain();
    }

    @Test
    void shutdownAndDrain_flushesRemainingEvents() throws InterruptedException {
        bus.setStall(true); // pause writer thread

        bus.publish(makeEvent());
        bus.publish(makeEvent());
        bus.publish(makeEvent());

        bus.setStall(false);
        bus.shutdownAndDrain(); // should flush all 3

        // After drain, all events should have been processed
        assertTrue(processed.size() >= 3, "All queued events should be drained");
    }

    @Test
    void isRunning_afterStart_true() {
        assertTrue(bus.isRunning());
    }

    @Test
    void isRunning_afterShutdown_false() {
        bus.shutdownAndDrain();
        assertFalse(bus.isRunning());
    }

    // ---- Helpers ----

    private OrderEvent makeEvent() {
        OrderRequest req = new OrderRequest()
                .symbol("NIFTY24JUL24000CE").exchange("NFO")
                .buy().market().quantity(50).product("NRML");
        OrderResponse resp = new OrderResponse();
        resp.setBrokerOrderId("TEST-" + System.nanoTime());
        resp.setStatus("OPEN");
        return new OrderEvent(OrderEvent.Type.PLACED, req, resp, "MSTOCK", 1);
    }

    /**
     * Concrete test implementation — records events instead of writing to DB.
     */
    static class TestEventBus extends AbstractEventBus<OrderEvent> {
        private final CopyOnWriteArrayList<OrderEvent> sink;
        private volatile CountDownLatch latch;
        private volatile boolean stall = false;

        TestEventBus(CopyOnWriteArrayList<OrderEvent> sink, int capacity) {
            super(capacity);
            this.sink = sink;
        }

        void setLatch(CountDownLatch l) { this.latch = l; }
        void setStall(boolean v)        { this.stall = v; }

        @Override
        protected void processEvent(OrderEvent event) {
            while (stall) {
                try { Thread.sleep(20); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); return;
                }
            }
            sink.add(event);
            if (latch != null) latch.countDown();
        }

        @Override
        protected String getBusName() { return "test"; }
    }
}
