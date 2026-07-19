package com.fnooms.algo;

public class AlgoManager {
    private static AlgoManager instance;
    private AlgoOrchestrator orchestrator;

    private AlgoManager() {
    }

    public static synchronized AlgoManager getInstance() {
        if (instance == null) {
            instance = new AlgoManager();
        }
        return instance;
    }

    public void start() {
        if (orchestrator == null) {
            orchestrator = new AlgoOrchestrator();
            // Start in a background thread so we don't block Tomcat startup
            new Thread(() -> {
                try {
                    orchestrator.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, "Algo-Orchestrator-Thread").start();
        }
    }

    public void stop() {
        if (orchestrator != null) {
            orchestrator.stop();
            orchestrator = null;
        }
    }

    public AlgoOrchestrator getOrchestrator() {
        return orchestrator;
    }

    public StrategyEngine getEngine() {
        return orchestrator != null ? orchestrator.getEngine() : null;
    }

    public MarketDataListener getListener() {
        return orchestrator != null ? orchestrator.getListener() : null;
    }
}
