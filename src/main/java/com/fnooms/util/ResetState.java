package com.fnooms.util;

import com.fnooms.dao.AlgoKeyValueDAO;

public class ResetState {
    public static void main(String[] args) {
        AlgoKeyValueDAO dao = new AlgoKeyValueDAO();
        // Set the state back to fully un-entered/exited
        dao.setValue("tradestate.NIFTY21JUL2624200CE", "{\"isEntered\":false,\"isExited\":false,\"currentStopLoss\":0.0,\"entryAttempts\":0}", "SYSTEM");
        System.out.println("Trade state successfully reset.");
    }
}
