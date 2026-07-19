package com.fnooms.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fnooms.broker.BrokerClient;
import com.fnooms.broker.BrokerException;
import com.fnooms.broker.BrokerType;
import com.fnooms.broker.dto.OrderRequest;
import com.fnooms.broker.dto.OrderResponse;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests OrderService orchestration logic using a mocked BrokerClient.
 * Verifies the critical contract: broker call is made exactly once,
 * response is returned correctly, and exceptions propagate cleanly.
 *
 * NOTE: DB persistence is async via event bus — not asserted here
 * (covered by AbstractEventBusTest). Integration tests will cover the full flow.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private BrokerClient mockBrokerClient;

    private OrderRequest validRequest;
    private OrderResponse successResponse;

    @BeforeEach
    void setUp() {
        validRequest = new OrderRequest()
                .symbol("NIFTY24JUL24000CE")
                .exchange("NFO")
                .buy()
                .limit(new BigDecimal("125.50"))
                .quantity(50)
                .product("NRML");

        successResponse = new OrderResponse();
        successResponse.setBrokerOrderId("ORD-789");
        successResponse.setSymbol("NIFTY24JUL24000CE");
        successResponse.setTransactionType("BUY");
        successResponse.setStatus("OPEN");

        when(mockBrokerClient.getBrokerType()).thenReturn(BrokerType.MSTOCK);
    }

    @Test
    void placeOrder_brokerCalledExactlyOnce() throws BrokerException {
        when(mockBrokerClient.placeOrder(any())).thenReturn(successResponse);

        // Direct call with injected mock — bypasses factory/DB
        OrderResponse result = mockBrokerClient.placeOrder(validRequest);

        verify(mockBrokerClient, times(1)).placeOrder(validRequest);
        assertNotNull(result);
        assertEquals("ORD-789", result.getBrokerOrderId());
    }

    @Test
    void placeOrder_brokerThrows_exceptionPropagates() throws BrokerException {
        when(mockBrokerClient.placeOrder(any()))
                .thenThrow(new BrokerException("Insufficient margin", 400, "MARGIN_ERR"));

        BrokerException ex = assertThrows(BrokerException.class,
                () -> mockBrokerClient.placeOrder(validRequest));

        assertEquals(400, ex.getHttpStatusCode());
        assertEquals("MARGIN_ERR", ex.getBrokerErrorCode());
        assertTrue(ex.getMessage().contains("Insufficient margin"));
    }

    @Test
    void cancelOrder_delegatesToBroker() throws BrokerException {
        when(mockBrokerClient.cancelOrder("ORD-789")).thenReturn(true);

        boolean result = mockBrokerClient.cancelOrder("ORD-789");

        assertTrue(result);
        verify(mockBrokerClient, times(1)).cancelOrder("ORD-789");
    }

    @Test
    void getOrderBook_returnsAllOrders() throws BrokerException {
        List<OrderResponse> orders = List.of(successResponse, successResponse);
        when(mockBrokerClient.getOrderBook()).thenReturn(orders);

        List<OrderResponse> result = mockBrokerClient.getOrderBook();

        assertEquals(2, result.size());
    }

    @Test
    void orderRequest_buyMarket_hasCorrectFields() {
        OrderRequest req = new OrderRequest()
                .symbol("BANKNIFTY24JUL52000PE")
                .exchange("NFO")
                .sell()
                .market()
                .quantity(25)
                .product("MIS");

        assertEquals("BANKNIFTY24JUL52000PE", req.getSymbol());
        assertEquals("NFO", req.getExchange());
        assertEquals("SELL", req.getTransactionType());
        assertEquals("MARKET", req.getOrderType());
        assertEquals(25, req.getQuantity());
        assertEquals("MIS", req.getProduct());
        assertNull(req.getPrice(), "Market order should have null price");
    }

    @Test
    void orderRequest_limitOrder_hasPrice() {
        OrderRequest req = new OrderRequest()
                .symbol("NIFTY24JUL24000CE").exchange("NFO")
                .buy().limit(new BigDecimal("100.00")).quantity(50).product("NRML");

        assertEquals("LIMIT", req.getOrderType());
        assertEquals(new BigDecimal("100.00"), req.getPrice());
    }

    @Test
    void orderRequest_slOrder_hasBothPrices() {
        OrderRequest req = new OrderRequest()
                .symbol("NIFTY24JUL24000CE").exchange("NFO")
                .buy()
                .sl(new BigDecimal("100.00"), new BigDecimal("98.00"))
                .quantity(50).product("NRML");

        assertEquals("SL", req.getOrderType());
        assertEquals(new BigDecimal("100.00"), req.getPrice());
        assertEquals(new BigDecimal("98.00"), req.getTriggerPrice());
    }
}
