package com.fnooms.algo;

public class AlgoOrchestratorMain {
    public static void main(String[] args) {
        System.setProperty("app.updater", "BATCH");
        com.fnooms.util.MStockScripMasterFetcher.fetchAndStoreScripMaster("mstock");
        com.fnooms.util.DhanScripMasterFetcher.fetchAndStoreScripMaster();
        AlgoOrchestrator orchestrator = new AlgoOrchestrator();
        orchestrator.start();
    }
}
