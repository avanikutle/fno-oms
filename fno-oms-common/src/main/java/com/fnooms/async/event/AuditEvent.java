package com.fnooms.async.event;

import java.time.Instant;

/**
 * Event published to the AuditEventBus for every broker API call.
 */
public class AuditEvent {

    private final String  action;
    private final String  brokerType;
    private final String  endpoint;
    private final String  request;
    private final String  response;
    private final int     statusCode;
    private final long    latencyMs;
    private final String  error;
    private final Instant eventTime;

    public AuditEvent(String action, String brokerType, String endpoint,
                      String request, String response, int statusCode,
                      long latencyMs, String error) {
        this.action     = action;
        this.brokerType = brokerType;
        this.endpoint   = endpoint;
        this.request    = request;
        this.response   = response;
        this.statusCode = statusCode;
        this.latencyMs  = latencyMs;
        this.error      = error;
        this.eventTime  = Instant.now();
    }

    public String  getAction()     { return action; }
    public String  getBrokerType() { return brokerType; }
    public String  getEndpoint()   { return endpoint; }
    public String  getRequest()    { return request; }
    public String  getResponse()   { return response; }
    public int     getStatusCode() { return statusCode; }
    public long    getLatencyMs()  { return latencyMs; }
    public String  getError()      { return error; }
    public Instant getEventTime()  { return eventTime; }
}
