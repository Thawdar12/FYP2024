package com.fyp14.OnePay.Controller;

import com.fyp14.OnePay.Wallets.Wallet;
import com.fyp14.OnePay.Wallets.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fyp14.OnePay.Transaction.TransactionService;


import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class WalletController {

    @Autowired
    private WalletService walletService;

    // Endpoint to get the current wallet info (ID and balance)
    @GetMapping("/user/wallet")
    public ResponseEntity<?> getUserWallet() {
        try {
            // Fetch the wallet information for the current user
            Wallet wallet = walletService.getWalletByUserId(1L);  // Adjust user ID handling as needed
            Map<String, Object> response = new HashMap<>();
            response.put("walletId", wallet.getId());
            response.put("balance", wallet.getBalance());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Collections.singletonMap("error", "Failed to retrieve wallet info."));
        }
    }


    // Inject the TransactionService instance
    @Autowired
    private TransactionService TransactionService;  // This is an instance, not a static reference

    // Endpoint to handle wallet transfers
    @PostMapping("/wallet-transfers")
    public ResponseEntity<?> transferFunds(@RequestBody TransferRequest transferRequest) {
        try {
            String result = TransactionService.transfer(
                    transferRequest.getSenderWalletId(),
                    transferRequest.getRecipientWalletId(),
                    BigDecimal.valueOf(transferRequest.getAmount())
            );
            return ResponseEntity.ok(Collections.singletonMap("status", "success"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("message", e.getMessage()));
        }
    }
}