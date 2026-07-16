package com.fnooms.algo;

public interface MarketDataListener {
    void start();
    void stop();
    void addSubscription(String symbol);
}
