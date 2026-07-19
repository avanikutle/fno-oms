package com.fnooms.broker.mstock.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles mStock API endpoints for User (Profile, Funds, Login).
 */
public class MStockUserApi {
    private static final Logger log = LoggerFactory.getLogger(MStockUserApi.class);
    private final MStockCoreClient core;

    public MStockUserApi(MStockCoreClient core) {
        this.core = core;
    }

    // STUBS
    // public void getProfile() {}
    // public void getFunds() {}
}
