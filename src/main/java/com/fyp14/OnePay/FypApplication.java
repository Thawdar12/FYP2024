package com.fyp14.OnePay;

import com.fyp14.OnePay.Security.KeyManagementService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.security.NoSuchAlgorithmException;

@SpringBootApplication
public class FypApplication {

    public static void main(String[] args) throws NoSuchAlgorithmException {
        SpringApplication.run(FypApplication.class, args);
        KeyManagementService kms = new KeyManagementService();
        String masterKEK = kms.generateMasterKEK();
        System.out.println("Master KEK (Base64 Encoded): " + masterKEK);
    }

}
