package com.fnooms.broker.mstock;

import com.fnooms.broker.BrokerException;
import com.fnooms.broker.dto.OrderRequest;
import com.fnooms.broker.dto.OrderResponse;
import com.fnooms.broker.dto.Quote;
import com.fnooms.model.BrokerConfig;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests MStockBrokerClient using OkHttp MockWebServer.
 * No real network calls — all responses are mocked locally.
 * Covers: order placement, quote parsing, error handling.
 */
class MStockBrokerClientTest {

    private static MockWebServer server;
    private MStockBrokerClient client;

    @BeforeAll
    static void startServer() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterAll
    static void stopServer() throws IOException {
        server.shutdown();
    }

    @BeforeEach
    void setUp() {
        // Point client at the local mock server
        BrokerConfig cfg = new BrokerConfig();
        cfg.setApiKey("test-api-key-12345");
        cfg.setPrivateKey("test-private-key");
        cfg.setAccessToken("test-jwt-access-token");
        cfg.setTokenExpiry(Instant.now().plusSeconds(3600));
        cfg.setBrokerType("MSTOCK");

        // Override base URL to mock server
        client = new MStockBrokerClientTestable(cfg, server.url("/").toString());
    }

    // ──────────────────────────────────────────────
    // ORDER PLACEMENT
    // ──────────────────────────────────────────────

    @Test
    void placeOrder_success_returnsOrderId() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"status": true, "data": {"order_id": "ORD123456"}}
                        """));

        OrderRequest req = new OrderRequest()
                .symbol("NIFTY24JUL24000CE").exchange("NFO")
                .buy().market().quantity(50).product("NRML");

        OrderResponse resp = client.placeOrder(req);

        assertEquals("ORD123456", resp.getBrokerOrderId());
        assertEquals("NIFTY24JUL24000CE", resp.getSymbol());
        assertEquals("BUY", resp.getTransactionType());
        assertEquals("OPEN", resp.getStatus());
    }

    @Test
    void placeOrder_brokerReturnsStatusFalse_throwsBrokerException() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"status": false, "message": "Insufficient margin"}
                        """));

        OrderRequest req = new OrderRequest()
                .symbol("NIFTY24JUL24000CE").exchange("NFO")
                .buy().market().quantity(50).product("NRML");

        BrokerException ex = assertThrows(BrokerException.class, () -> client.placeOrder(req));
        assertTrue(ex.getMessage().contains("Insufficient margin"));
    }

    @Test
    void placeOrder_http401_throwsBrokerException() {
        server.enqueue(new MockResponse()
                .setResponseCode(401)
                .setBody("{\"message\": \"Unauthorized\"}"));

        OrderRequest req = new OrderRequest()
                .symbol("NIFTY24JUL24000CE").exchange("NFO")
                .buy().market().quantity(50).product("NRML");

        BrokerException ex = assertThrows(BrokerException.class, () -> client.placeOrder(req));
        assertEquals(401, ex.getHttpStatusCode());
    }

    @Test
    void placeOrder_sendsCorrectAuthHeaders() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"status\": true, \"data\": {\"order_id\": \"X1\"}}"));

        OrderRequest req = new OrderRequest()
                .symbol("NIFTY24JUL24000CE").exchange("NFO")
                .buy().market().quantity(50).product("NRML");
        client.placeOrder(req);

        RecordedRequest recorded = server.takeRequest();
        assertNotNull(recorded.getHeader("Authorization"));
        assertTrue(recorded.getHeader("Authorization").startsWith("token "));
        assertEquals("1", recorded.getHeader("X-Mirae-Version"));
        assertNotNull(recorded.getHeader("X-PrivateKey"));
    }

    // ──────────────────────────────────────────────
    // QUOTES
    // ──────────────────────────────────────────────

    @Test
    void getQuotes_success_parsesLtpAndOi() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "status": true,
                          "data": {
                            "NFO:NIFTY24JUL24000CE": {
                              "last_price": 125.50,
                              "volume": 50000,
                              "oi": 1234567,
                              "net_change": 5.25,
                              "ohlc": {"open": 120.0, "high": 130.0, "low": 118.0, "close": 120.25}
                            }
                          }
                        }
                        """));

        Map<String, Quote> quotes = client.getQuotes(List.of("NFO:NIFTY24JUL24000CE"));

        assertEquals(1, quotes.size());
        Quote q = quotes.get("NFO:NIFTY24JUL24000CE");
        assertNotNull(q);
        assertEquals(new BigDecimal("125.50"), q.getLtp());
        assertEquals(1234567L, q.getOi());
        assertEquals("NIFTY24JUL24000CE", q.getSymbol());
        assertEquals("NFO", q.getExchange());
    }

    @Test
    void getQuotes_emptyList_returnsEmptyMap() throws Exception {
        Map<String, Quote> quotes = client.getQuotes(List.of());
        assertTrue(quotes.isEmpty());
    }

    // ──────────────────────────────────────────────
    // CANCEL ORDER
    // ──────────────────────────────────────────────

    @Test
    void cancelOrder_success_returnsTrue() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"status\": true}"));

        boolean result = client.cancelOrder("ORD123456");
        assertTrue(result);

        RecordedRequest req = server.takeRequest();
        assertEquals("DELETE", req.getMethod());
        assertTrue(req.getPath().contains("ORD123456"));
    }

    // ──────────────────────────────────────────────
    // TEST HELPER: Overrides base URL
    // ──────────────────────────────────────────────

    /**
     * Subclass that lets us inject a custom base URL for MockWebServer.
     */
    static class MStockBrokerClientTestable extends MStockBrokerClient {
        private final String baseUrl;

        MStockBrokerClientTestable(BrokerConfig cfg, String baseUrl) {
            super(cfg);
            // Strip trailing slash
            this.baseUrl = baseUrl.endsWith("/")
                    ? baseUrl.substring(0, baseUrl.length() - 1)
                    : baseUrl;
        }

        @Override
        protected String getBaseUrl() {
            return baseUrl;
        }
    }
}
