package com.fyp14.OnePay.Controller;

import com.fyp14.OnePay.Wallets.Wallet;
import com.fyp14.OnePay.Wallets.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TopUpController {

    @Autowired
    private WalletService walletService;

    // We now pass walletId directly in the request body along with the amount.
    @PostMapping("/topup")
    public ResponseEntity<?> topUpWallet(@RequestBody TopUpRequest topUpRequest) {
        // Retrieve walletId from the request body instead of the session
        Long walletId = topUpRequest.getWalletId();  // Ensure walletId is in TopUpRequest

        if (walletId == null) {
            return ResponseEntity.badRequest().body("Wallet ID is missing");
        }

        if (topUpRequest.getAmount() == null || topUpRequest.getAmount() <= 0) {
            return ResponseEntity.badRequest().body("Invalid top-up amount");
        }

        BigDecimal amount = BigDecimal.valueOf(topUpRequest.getAmount());

        // Call the wallet service to top up the wallet
        String result = walletService.topUpWallet(walletId, amount);

        if (result.equals("Top-up successful")) {
            return ResponseEntity.ok(Collections.singletonMap("status", "success"));
        } else {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", result));
        }
    }

    // We now pass walletId as a query parameter to fetch the wallet balance.
    @GetMapping("/wallet")
    public ResponseEntity<?> getUserWallet() {
        try {
            Wallet wallet = walletService.getWalletByUserId(1L);  // Replace with dynamically fetched user ID
            Map<String, Object> response = new HashMap<>();
            response.put("walletId", wallet.getId());
            response.put("balance", wallet.getBalance());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Collections.singletonMap("error", "Failed to retrieve wallet info."));
        }
    }

}
