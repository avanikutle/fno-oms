package com.fnooms.service;

import com.fnooms.broker.BrokerClient;
import com.fnooms.broker.BrokerClientFactory;
import com.fnooms.broker.BrokerException;
import com.fnooms.broker.dto.Holding;
import com.fnooms.broker.dto.Position;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
