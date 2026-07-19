package com.fnooms.algo.login;

import com.fnooms.dao.AlgoKeyValueDAO;
import java.util.Scanner;

public class DhanLoginMain {

    public static void main(String[] args) {
        System.setProperty("app.updater", "BATCH");
        AlgoKeyValueDAO dao = new AlgoKeyValueDAO();
        String clientId = dao.getValue("dhan.client_id");

        if (clientId == null || clientId.equals("DHAN_CLIENT_ID")) {
            System.err.println("Error: Please set your actual dhan.client_id in algo_key_value table.");
            return;
        }

        Scanner scanner = new Scanner(System.in);
        System.out.print("Do you want to reset authentication? (y/n): ");
        String resetAuth = scanner.nextLine().trim();

        if (resetAuth.equalsIgnoreCase("y")) {
            System.out.print("Enter 6-digit PIN for Dhan: ");
            String pin = scanner.nextLine().trim();

            System.out.print("Enter 6-digit TOTP for Dhan: ");
            String totp = scanner.nextLine().trim();

            DhanLogin dhanLogin = new DhanLogin();
            try {
                System.out.println("Attempting login...");
                String accessToken = dhanLogin.login(clientId, pin, totp);
                System.out.println("SUCCESS! Access Token: " + accessToken);
                dao.setValue("dhan.access_token", accessToken);
                System.out.println("Token saved to DB successfully.");
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        } else {
            String existingToken = dao.getValue("dhan.access_token");
            if (existingToken == null || existingToken.isEmpty()) {
                System.err.println("Error: No existing token found in DB. Please run again and reset authentication.");
                return;
            }
            System.out.println("Using existing access token from DB.");
        }

        com.fnooms.util.DhanScripMasterFetcher.fetchAndStoreScripMaster();
        System.out.println("Scrip master downloaded.");
    }
}
