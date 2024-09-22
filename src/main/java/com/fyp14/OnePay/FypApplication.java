package com.fyp14.OnePay;

import com.fyp14.OnePay.KeyManagement.KEK.KeyManagementService;
import com.fyp14.OnePay.User.User;
import com.fyp14.OnePay.User.UserRepository;
import com.fyp14.OnePay.User.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.security.NoSuchAlgorithmException;

@SpringBootApplication
public class FypApplication implements CommandLineRunner {

    // Inject the KeyManagementService managed by Spring
    private final KeyManagementService keyManagementService;
    private final UserService userService;
    private final UserRepository userRepository;

    public FypApplication(KeyManagementService keyManagementService, UserService userService, UserRepository userRepository) {
        this.keyManagementService = keyManagementService;
        this.userService = userService;
        this.userRepository = userRepository;
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

            // Check if user 'adwwee' exists, purely for me been lazy to make new user every time I drop the table
            String username = "adwwee";
            String username2 = "test";
            userRepository.findByUsername(username).ifPresentOrElse(user -> {
                System.out.println("User 'adwwee' already exists.");
            }, () -> {
                System.out.println("User 'adwwee' not found. Registering new user...");
                try {
                    // Create a new user
                    User newUser = new User();
                    newUser.setUsername(username);
                    newUser.setPassword("@Aa3609773");
                    newUser.setPhoneNumber("90215389");
                    newUser.setEmail("adwwee@example.com");

                    User newUser2 = new User();
                    newUser2.setUsername(username2);
                    newUser2.setPassword("@Aa3609773");
                    newUser2.setPhoneNumber("12345678");
                    newUser2.setEmail("test@example.com");

                    // Register user through the service
                    userService.registerUser(newUser);
                    userService.registerUser(newUser2);

                    System.out.println("User 'adwwee' and 'test' successfully registered.");
                } catch (Exception e) {
                    System.err.println("Error registering user 'adwwee' and 'test': " + e.getMessage());
                    e.printStackTrace();
                }
            });
        }
    }
}