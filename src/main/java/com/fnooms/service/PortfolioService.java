package com.fnooms.service;

import com.fnooms.broker.BrokerClient;
import com.fnooms.broker.BrokerClientFactory;
import com.fnooms.broker.BrokerException;
import com.fnooms.broker.dto.Holding;
import com.fnooms.broker.dto.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class PortfolioService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioService.class);

    public List<Position> getPositions() throws BrokerException {
        BrokerClient client = BrokerClientFactory.getActiveClient();
        return client.getPositions();
    }

    public List<Holding> getHoldings() throws BrokerException {
        BrokerClient client = BrokerClientFactory.getActiveClient();
        return client.getHoldings();
    }
}
