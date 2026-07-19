package com.fnooms.util;

import com.fnooms.dao.AlgoKeyValueDAO;

public class ResetState {
    public static void main(String[] args) {
        AlgoKeyValueDAO dao = new AlgoKeyValueDAO();
        // Set the state back to fully un-entered/exited
        String symbol = "NIFTY2672124000PE"; // Change this symbol to the one you want to reset
        dao.setValue("state." + symbol + ".entered", "false");
        dao.setValue("state." + symbol + ".exited", "false");
        System.out.println("Trade state successfully reset for " + symbol);
    }
}
