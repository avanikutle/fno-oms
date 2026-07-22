package com.fnooms.broker;

import com.fnooms.model.BrokerContext;
import com.fnooms.model.StandardHolding;
import java.util.List;

/**
 * Standardized interface for interacting with different brokers.
 */
public interface IBrokerApiClient {
    
    /**
     * Initializes the client with a specific broker context (credentials/tokens).
     */
    void init(BrokerContext context);
    
    /**
     * Performs authentication/login and updates the context with new tokens if applicable.
     */
    boolean login() throws Exception;
    
    /**
     * Retrieves the holdings for the current user context.
     */
    List<StandardHolding> getHoldings() throws Exception;
    
    /**
     * Retrieves the user details for the current user context.
     */
    String getUserDetails() throws Exception;
    
}
