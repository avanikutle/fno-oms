package com.fnooms.algo.login;

import com.fnooms.dao.AlgoKeyValueDAO;

import java.util.Scanner;

public class MStockLoginMain {

    public static void main(String[] args) {
        AlgoKeyValueDAO dao = new AlgoKeyValueDAO();
        String userId = dao.getValue("mstock.userid");
        String password = dao.getValue("mstock.pdcred");
        String apiKey = dao.getValue("mstock.api_key");

        if (userId == null || password == null || apiKey == null) {
            System.err.println("Error: mStock credentials (mstock.userid, mstock.pdcred, mstock.api_key) not found in algo_key_value table.");
            return;
        }

        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter 6-digit TOTP for mStock: ");
        String totp = scanner.nextLine().trim();

        MStockLogin mStockLogin = new MStockLogin();
        try {
            System.out.println("Attempting login...");
            String token = mStockLogin.login(userId, password, totp, apiKey);
            System.out.println("SUCCESS! JWT Token: " + token);
            dao.setValue("mstock.jwt_token", token, "STANDALONE");
            System.out.println("Token saved to DB successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
