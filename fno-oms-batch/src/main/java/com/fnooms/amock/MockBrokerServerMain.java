package com.fnooms.amock;

public class MockBrokerServerMain {
    public static void main(String[] args) {
        MockBrokerServer.start();
        
        // Add shutdown hook to save state gracefully
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down MockBrokerServer...");
            // The saveState is already called on every order placement, 
            // but we can add more cleanup here if needed.
        }));
    }
}
