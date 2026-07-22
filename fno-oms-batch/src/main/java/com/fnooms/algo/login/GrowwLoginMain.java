package com.fnooms.algo.login;

import com.fnooms.broker.GrowwApiClient;
import com.fnooms.broker.IBrokerApiClient;
import com.fnooms.dao.AlgoKeyValueDAO;
import com.fnooms.model.BrokerContext;
import com.fnooms.model.StandardHolding;
import java.util.List;
import java.util.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrowwLoginMain {
    private static final Logger log = LoggerFactory.getLogger(GrowwLoginMain.class);
    
    public static void main(String[] args) {
        log.info("--- Groww Interactive Login ---");
        AlgoKeyValueDAO dao = new AlgoKeyValueDAO();
        
        try {
            // Read API key from DB
            String apiKey = dao.getValue("groww.api_key");
            if (apiKey == null || apiKey.trim().isEmpty()) {
                log.error("groww.api_key is missing in algo_key_value table.");
                System.out.println("Please insert 'groww.api_key' into algo_key_value table and try again.");
                return;
            }
            
            Scanner scanner = new Scanner(System.in);
            System.out.print("Enter Groww TOTP code: ");
            String totp = scanner.nextLine().trim();
            
            BrokerContext context = new BrokerContext();
            context.setBrokerType("GROWW");
            context.setApiKey(apiKey);
            context.setTotp(totp);
            
            IBrokerApiClient growwClient = new GrowwApiClient();
            growwClient.init(context);
            
            if (growwClient.login()) {
                log.info("Login successful.");
                
                // Save access token back to database
                if (context.getAccessToken() != null) {
                    dao.setValue("groww.access_token", context.getAccessToken());
                    log.info("Saved new access token to algo_key_value.");
                }
                
                System.out.print("\nDo you want to download and store the Groww Scrip Master? (y/n): ");
                String choice = scanner.nextLine().trim().toLowerCase();
                
                if (choice.equals("y") || choice.equals("yes")) {
                    com.fnooms.util.GrowwScripMasterFetcher.fetchAndStoreScripMaster();
                } else {
                    log.info("Scrip master download skipped.");
                }

                log.info("\nFetching user details...");
                try {
                    String userDetails = growwClient.getUserDetails();
                    System.out.println("User Details: " + userDetails);
                } catch (Exception e) {
                    log.error("Could not fetch user details: " + e.getMessage());
                }

                log.info("\nFetching holdings...");
                try {
                    List<StandardHolding> holdings = growwClient.getHoldings();
                    System.out.println("--- Holdings ---");
                    for (StandardHolding holding : holdings) {
                        System.out.println(holding);
                    }
                } catch (Exception e) {
                    log.error("Could not fetch holdings (You might need specific scopes or permissions): " + e.getMessage());
                }
                
            } else {
                log.error("Groww login failed.");
            }
            
        } catch (Exception e) {
            log.error("An error occurred during Groww login flow", e);
        }
    }
}
