package com.fnooms.model;

import java.time.Instant;

/**
 * Represents an audit log entry for every broker API call.
 */
public class AuditLog {

    private long    id;
    private String  action;
    private String  brokerType;
    private String  endpoint;
    private String  request;
    private String  response;
    private int     statusCode;
    private long    latencyMs;
    private String  error;
    private Instant createdAt;

    public AuditLog() {}

    public AuditLog(String action, String brokerType, String endpoint) {
        this.action     = action;
        this.brokerType = brokerType;
        this.endpoint   = endpoint;
        this.createdAt  = Instant.now();
    }

    // ---- Getters & Setters ----
    public long    getId()                    { return id; }
    public void    setId(long v)              { this.id = v; }

    public String  getAction()               { return action; }
    public void    setAction(String v)       { this.action = v; }

    public String  getBrokerType()           { return brokerType; }
    public void    setBrokerType(String v)   { this.brokerType = v; }

    public String  getEndpoint()             { return endpoint; }
    public void    setEndpoint(String v)     { this.endpoint = v; }

    public String  getRequest()              { return request; }
    public void    setRequest(String v)      { this.request = v; }

    public String  getResponse()             { return response; }
    public void    setResponse(String v)     { this.response = v; }

    public int     getStatusCode()           { return statusCode; }
    public void    setStatusCode(int v)      { this.statusCode = v; }

    public long    getLatencyMs()            { return latencyMs; }
    public void    setLatencyMs(long v)      { this.latencyMs = v; }

    public String  getError()               { return error; }
    public void    setError(String v)       { this.error = v; }

    public Instant getCreatedAt()           { return createdAt; }
    public void    setCreatedAt(Instant v)  { this.createdAt = v; }
}
