package com.fnooms.broker.mstock.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles mStock API endpoints for Basket APIs.
 */
public class MStockBasketApi {
    private static final Logger log = LoggerFactory.getLogger(MStockBasketApi.class);
    private final MStockCoreClient core;

    public MStockBasketApi(MStockCoreClient core) {
        this.core = core;
    }

    // STUBS
    // public void createBasket() {}
    // public void getBaskets() {}
}
