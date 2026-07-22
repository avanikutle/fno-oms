package com.fnooms.model;

/**
 * Encapsulates credentials and context for a specific user and broker.
 */
public class BrokerContext {
    private String userId;
    private String brokerType;
    private String apiKey;
    private String accessToken;
    private String password;
    private String totp;
    
    // Default constructor
    public BrokerContext() {}

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getBrokerType() { return brokerType; }
    public void setBrokerType(String brokerType) { this.brokerType = brokerType; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getTotp() { return totp; }
    public void setTotp(String totp) { this.totp = totp; }
}
