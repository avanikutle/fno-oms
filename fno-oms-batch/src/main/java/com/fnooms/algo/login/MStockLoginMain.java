package com.fnooms.algo.login;

import com.fnooms.dao.AlgoKeyValueDAO;
import java.util.Scanner;

public class MStockLoginMain {

    public static void main(String[] args) {
        System.setProperty("app.updater", "BATCH");
        AlgoKeyValueDAO dao = new AlgoKeyValueDAO();
        String userId = dao.getValue("mstock.userid");
        String password = dao.getValue("mstock.pdcred");
        String apiKey = dao.getValue("mstock.api_key");

        if (userId == null || password == null || apiKey == null) {
            System.err.println("Error: mStock credentials (mstock.userid, mstock.pdcred, mstock.api_key) not found in algo_key_value table.");
            return;
        }

        Scanner scanner = new Scanner(System.in);
        System.out.print("Do you want to reset authentication? (y/n): ");
        String resetAuth = scanner.nextLine().trim();

        if (resetAuth.equalsIgnoreCase("y")) {
            System.out.print("Enter 6-digit TOTP for mStock: ");
            String totp = scanner.nextLine().trim();

            MStockLogin mStockLogin = new MStockLogin();
            try {
                System.out.println("Attempting login...");
                String[] tokens = mStockLogin.login(userId, password, totp, apiKey);
                System.out.println("SUCCESS! JWT Token: " + tokens[0]);
                dao.setValue("mstock.jwt_token", tokens[0]);
                dao.setValue("mstock.refresh_token", tokens[1]);
                System.out.println("Tokens saved to DB successfully.");
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        } else {
            String jwt = dao.getValue("mstock.jwt_token");
            if (jwt == null || jwt.isEmpty()) {
                System.err.println("Error: No existing JWT token found in DB. Please run again and reset authentication.");
                return;
            }
            System.out.println("Using existing JWT token from DB.");
        }

        com.fnooms.util.MStockScripMasterFetcher.fetchAndStoreScripMaster("mstock");
        System.out.println("Scrip master downloaded.");
    }
}
