package com.fnooms.algo;

public class AlgoOrchestratorMain {
    public static void main(String[] args) {
        com.fnooms.util.MStockScripMasterFetcher.fetchAndStoreScripMaster("mstock");
        AlgoOrchestrator orchestrator = new AlgoOrchestrator();
        orchestrator.start();
    }
}
