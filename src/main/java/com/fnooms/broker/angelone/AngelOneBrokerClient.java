package com.fnooms.broker.angelone;

import com.fnooms.broker.BrokerClient;
import com.fnooms.broker.BrokerException;
import com.fnooms.broker.BrokerType;
import com.fnooms.broker.dto.*;
import com.fnooms.util.CredsUtil;

import java.util.List;
import java.util.Map;

public class AngelOneBrokerClient implements BrokerClient {
    private final String prefix;

    public AngelOneBrokerClient(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public BrokerType getBrokerType() {
        return BrokerType.ANGEL;
    }

    @Override
    public void testConnection() throws BrokerException {
        // Just verify that the JWT token is present and credentials are loaded
        String jwtToken = CredsUtil.getJwtToken(prefix);
        if (jwtToken == null || jwtToken.isEmpty()) {
            throw new BrokerException("Broker is not fully configured (missing JWT token) for prefix: " + prefix);
        }
    }

    @Override
    public Map<String, Quote> getQuotes(List<String> instruments) throws BrokerException {
        throw new UnsupportedOperationException("AngelOne execution not implemented yet");
    }

    @Override
    public OrderResponse placeOrder(OrderRequest request) throws BrokerException {
        throw new UnsupportedOperationException("AngelOne execution not implemented yet");
    }

    @Override
    public List<OrderResponse> getOrderBook() throws BrokerException {
        throw new UnsupportedOperationException("AngelOne execution not implemented yet");
    }

    @Override
    public boolean cancelOrder(String brokerOrderId) throws BrokerException {
        throw new UnsupportedOperationException("AngelOne execution not implemented yet");
    }

    @Override
    public OrderResponse modifyOrder(String brokerOrderId, OrderRequest updated) throws BrokerException {
        throw new UnsupportedOperationException("AngelOne execution not implemented yet");
    }

    @Override
    public List<Position> getPositions() throws BrokerException {
        throw new UnsupportedOperationException("AngelOne execution not implemented yet");
    }

    @Override
    public List<Holding> getHoldings() throws BrokerException {
        throw new UnsupportedOperationException("AngelOne execution not implemented yet");
    }
}
