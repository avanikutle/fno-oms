package com.fnooms.broker.mstock;

import com.fnooms.broker.BrokerClient;
import com.fnooms.broker.BrokerException;
import com.fnooms.broker.BrokerType;
import com.fnooms.broker.dto.*;
import com.fnooms.broker.mstock.api.*;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * mStock (Mirae Asset) broker client implementation.
 * Acts as a facade delegating calls to specific modular API classes.
 */
public class MStockBrokerClient implements BrokerClient {

    private static final Logger log = LoggerFactory.getLogger(MStockBrokerClient.class);

    private final MStockConfig config;
    private final MStockCoreClient coreApi;
    
    // Modular APIs
    private final MStockOrdersApi ordersApi;
    private final MStockPortfolioApi portfolioApi;
    private final MStockMarketApi marketApi;
    private final MStockUserApi userApi;
    private final MStockBasketApi basketApi;
    private final MStockMarginApi marginApi;

    public MStockBrokerClient(String prefix) {
        this.config = new MStockConfig(prefix);
        OkHttpClient http = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
                
        this.coreApi = new MStockCoreClient(this.config, http);
        this.ordersApi = new MStockOrdersApi(coreApi);
        this.portfolioApi = new MStockPortfolioApi(coreApi);
        this.marketApi = new MStockMarketApi(coreApi);
        this.userApi = new MStockUserApi(coreApi);
        this.basketApi = new MStockBasketApi(coreApi);
        this.marginApi = new MStockMarginApi(coreApi);
    }

    protected String getBaseUrl() {
        return config.getBaseUrl();
    }

    @Override
    public BrokerType getBrokerType() {
        return BrokerType.MSTOCK;
    }

    // =========================================================
    //  DELEGATED: QUOTES (Market API)
    // =========================================================

    @Override
    public Map<String, Quote> getQuotes(List<String> instruments) throws BrokerException {
        return marketApi.getQuotes(instruments);
    }

    // =========================================================
    //  DELEGATED: ORDERS (Orders API)
    // =========================================================

    @Override
    public OrderResponse placeOrder(OrderRequest req) throws BrokerException {
        return ordersApi.placeOrder(req);
    }

    @Override
    public List<OrderResponse> getOrderBook() throws BrokerException {
        return ordersApi.getOrderBook();
    }

    @Override
    public boolean cancelOrder(String brokerOrderId) throws BrokerException {
        return ordersApi.cancelOrder(brokerOrderId);
    }

    @Override
    public OrderResponse modifyOrder(String brokerOrderId, OrderRequest updated) throws BrokerException {
        return ordersApi.modifyOrder(brokerOrderId, updated);
    }

    // =========================================================
    //  DELEGATED: PORTFOLIO (Portfolio API)
    // =========================================================

    @Override
    public List<Position> getPositions() throws BrokerException {
        return portfolioApi.getPositions();
    }

    @Override
    public List<Holding> getHoldings() throws BrokerException {
        return portfolioApi.getHoldings();
    }

    // =========================================================
    //  CONNECTIVITY TEST
    // =========================================================

    @Override
    public void testConnection() throws BrokerException {
        if (!config.isConfigured()) {
            throw new BrokerException("Broker is not fully configured (missing API key or tokens)");
        }
        // The mStock API does not have a generic /user/profile, 
        // so we test connectivity against /portfolio/holdings since it is known to work
        portfolioApi.getHoldings();
    }
}
