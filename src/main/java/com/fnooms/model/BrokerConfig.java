package com.fnooms.model;

import java.time.Instant;

/**
 * Represents a configured broker entry in the database.
 * Holds credentials and token state for a specific broker account.
 */
public class BrokerConfig {

    private int     id;
    private String  brokerType;        // e.g. "MSTOCK"
    private String  displayName;       // e.g. "My mStock Account"
    private String  apiKey;
    private String  privateKey;
    private String  accessToken;       // JWT, valid till midnight
    private Instant tokenExpiry;
    private String  clientId;          // Broker's client/user ID
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    // Transient: set after connectivity check, not persisted
    private transient Boolean connected;
    private transient Long    latencyMs;
    private transient String  connectionError;

    public BrokerConfig() {}

    // ---- Getters & Setters ----

    public int     getId()                         { return id; }
    public void    setId(int v)                    { this.id = v; }

    public String  getBrokerType()                 { return brokerType; }
    public void    setBrokerType(String v)         { this.brokerType = v; }

    public String  getDisplayName()                { return displayName; }
    public void    setDisplayName(String v)        { this.displayName = v; }

    public String  getApiKey()                     { return apiKey; }
    public void    setApiKey(String v)             { this.apiKey = v; }

    public String  getPrivateKey()                 { return privateKey; }
    public void    setPrivateKey(String v)         { this.privateKey = v; }

    public String  getAccessToken()                { return accessToken; }
    public void    setAccessToken(String v)        { this.accessToken = v; }

    public Instant getTokenExpiry()                { return tokenExpiry; }
    public void    setTokenExpiry(Instant v)       { this.tokenExpiry = v; }

    public String  getClientId()                   { return clientId; }
    public void    setClientId(String v)           { this.clientId = v; }

    public boolean isActive()                      { return active; }
    public void    setActive(boolean v)            { this.active = v; }

    public Instant getCreatedAt()                  { return createdAt; }
    public void    setCreatedAt(Instant v)         { this.createdAt = v; }

    public Instant getUpdatedAt()                  { return updatedAt; }
    public void    setUpdatedAt(Instant v)         { this.updatedAt = v; }

    // ---- Transient connectivity state ----

    public Boolean  getConnected()                 { return connected; }
    public void     setConnected(Boolean v)        { this.connected = v; }

    public Long     getLatencyMs()                 { return latencyMs; }
    public void     setLatencyMs(Long v)           { this.latencyMs = v; }

    public String   getConnectionError()           { return connectionError; }
    public void     setConnectionError(String v)   { this.connectionError = v; }

    /** Mask the access token for safe display (show only last 6 chars). */
    public String getMaskedToken() {
        if (accessToken == null || accessToken.length() < 6) return "****";
        return "****" + accessToken.substring(accessToken.length() - 6);
    }

    /** Mask the API key for safe display. */
    public String getMaskedApiKey() {
        if (apiKey == null || apiKey.length() < 4) return "****";
        return apiKey.substring(0, 4) + "****";
    }

    public boolean isTokenExpired() {
        return tokenExpiry == null || Instant.now().isAfter(tokenExpiry);
    }
}
