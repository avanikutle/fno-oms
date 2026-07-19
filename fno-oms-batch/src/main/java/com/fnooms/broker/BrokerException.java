package com.fnooms.broker;

/**
 * Checked exception for all broker API errors.
 */
public class BrokerException extends Exception {

    private final int httpStatusCode;
    private final String brokerErrorCode;

    public BrokerException(String message) {
        super(message);
        this.httpStatusCode = -1;
        this.brokerErrorCode = null;
    }

    public BrokerException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatusCode = -1;
        this.brokerErrorCode = null;
    }

    public BrokerException(String message, int httpStatusCode, String brokerErrorCode) {
        super(message);
        this.httpStatusCode = httpStatusCode;
        this.brokerErrorCode = brokerErrorCode;
    }

    public int getHttpStatusCode() { return httpStatusCode; }
    public String getBrokerErrorCode() { return brokerErrorCode; }

    @Override
    public String toString() {
        return "BrokerException{message=" + getMessage()
                + ", httpStatus=" + httpStatusCode
                + ", brokerCode=" + brokerErrorCode + "}";
    }
}
