package com.fnooms.util;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class AngelOneScripMasterFetcher {
    // The public URL provided by Angel One for daily Scrip Master Data
    private static final String SCRIP_MASTER_URL = "https://margincalculator.angelbroking.com/OpenAPI_File/files/OpenAPIScripMaster.json";
    
    // File will be saved in the root directory by default
    private static final String FILE_NAME = "angelone_scripmaster.json";

    public static void fetchAndSaveScripMaster() {
        try {
            System.out.println("Starting download of Angel One Scrip Master JSON from: " + SCRIP_MASTER_URL);
            URL url = new URL(SCRIP_MASTER_URL);
            
            // Get the target path
            Path targetPath = Paths.get(FILE_NAME);
            
            // This will download the file and replace it if it already exists
            try (InputStream in = url.openStream()) {
                Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Successfully downloaded and saved to: " + targetPath.toAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("Error fetching scrip master: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        fetchAndSaveScripMaster();
    }
}
