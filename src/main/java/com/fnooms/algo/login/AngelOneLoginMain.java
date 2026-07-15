package com.fnooms.algo.login;

import com.fnooms.dao.AlgoKeyValueDAO;

import java.util.Scanner;

public class AngelOneLoginMain {

    public static void main(String[] args) {
        AlgoKeyValueDAO dao = new AlgoKeyValueDAO();
        String userId = dao.getValue("angelone.client_code");
        String password = dao.getValue("angelone.pdcred");
        String apiKey = dao.getValue("angelone.api_key");

        if (userId == null || password == null || apiKey == null) {
            System.err.println("Error: AngelOne credentials (angelone.client_code, angelone.pdcred, angelone.api_key) not found in algo_key_value table.");
            return;
        }

        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter 6-digit TOTP for AngelOne: ");
        String totp = scanner.nextLine().trim();

        AngelOneLogin angelOneLogin = new AngelOneLogin();
        try {
            System.out.println("Attempting login...");
            String[] tokens = angelOneLogin.login(userId, password, totp, apiKey);
            System.out.println("SUCCESS! JWT Token: " + tokens[0]);
            System.out.println("SUCCESS! Feed Token: " + tokens[1]);
            dao.setValue("angelone.jwt_token", tokens[0], "STANDALONE");
            dao.setValue("angelone.feed_token", tokens[1], "STANDALONE");
            System.out.println("Token saved to DB successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
