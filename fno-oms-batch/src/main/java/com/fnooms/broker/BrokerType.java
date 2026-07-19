package com.fnooms.broker;

/**
 * Enum of supported broker integrations.
 * Add new entries here as new brokers are integrated.
 */
public enum BrokerType {
    MSTOCK("mStock (Mirae Asset)"),
    DHAN("Dhan"),                   // Added Dhan integration
    ZERODHA("Zerodha Kite"),        // Future integration
    UPSTOX("Upstox"),               // Future integration
    ANGEL("Angel One"),             // Future integration
    FYERS("Fyers");                 // Future integration

    private final String displayName;

    BrokerType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static BrokerType fromString(String value) {
        for (BrokerType bt : values()) {
            if (bt.name().equalsIgnoreCase(value)) {
                return bt;
            }
        }
        throw new IllegalArgumentException("Unknown broker type: " + value);
    }
}
