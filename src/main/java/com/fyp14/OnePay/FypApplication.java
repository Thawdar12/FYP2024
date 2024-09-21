package com.fyp14.OnePay;

import com.fyp14.OnePay.Security.KeyManagementService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.security.NoSuchAlgorithmException;

@SpringBootApplication
public class FypApplication implements CommandLineRunner {

    // Inject the KeyManagementService managed by Spring
    private final KeyManagementService keyManagementService;

    public FypApplication(KeyManagementService keyManagementService) {
        this.keyManagementService = keyManagementService;
    }

    public static void main(String[] args) {
        SpringApplication.run(FypApplication.class, args);
    }

    @Override
    public void run(String... args) throws NoSuchAlgorithmException {
        // Check if the command line argument contains "-rotate"
        if (args.length > 0 && args[0].equalsIgnoreCase("-rotate")) {
            System.out.println("Rotation command detected. Rotating Master KEK...");
            try {
                keyManagementService.rotateMasterKEK();
                System.out.println("Key Rotation completed, system will shutdown in 10 seconds.\nPlease remember to update the Master Key.");
                Thread.sleep(10 * 1000);
                System.out.println("Shutting down now.");
                System.exit(0);
            } catch (Exception e) {
                System.err.println("Error occurred during Master KEK rotation: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            // Default behavior if no command or different command is passed
            String masterKEK = keyManagementService.generateMasterKEK();
            System.out.println("Master KEK (Base64 Encoded): " + masterKEK);
        }
    }
}
