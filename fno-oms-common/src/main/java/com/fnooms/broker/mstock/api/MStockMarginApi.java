package com.fnooms.broker.mstock.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles mStock API endpoints for Calculate Order Margin.
 */
public class MStockMarginApi {
    private static final Logger log = LoggerFactory.getLogger(MStockMarginApi.class);
    private final MStockCoreClient core;

    public MStockMarginApi(MStockCoreClient core) {
        this.core = core;
    }

    // STUBS
    // public void calculateMargin() {}
}
