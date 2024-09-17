package com.fyp14.OnePay.Controller;

import com.fyp14.OnePay.Transaction.TransactionService;
import com.fyp14.OnePay.Wallets.Wallet;
import com.fyp14.OnePay.Wallets.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal; // <-- Import for BigDecimal
import java.util.Collections;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api")
public class PaymentController {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private WalletRepository walletRepository; // <-- Properly inject the WalletRepository

    @PostMapping("/transfer")
    public ResponseEntity<?> transfer(@RequestBody TransferRequest transferRequest) {
        // Convert the Double amount to BigDecimal before passing it to the service
        BigDecimal amount = BigDecimal.valueOf(transferRequest.getAmount());

        String result = transactionService.transfer(
                transferRequest.getSenderWalletId(),
                transferRequest.getRecipientWalletId(),
                amount // Pass BigDecimal
        );

        if (result.equals("Transfer successful")) {
            return ResponseEntity.ok().body(Collections.singletonMap("status", "success"));
        } else {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", result));
        }
    }

    @GetMapping("/wallet/{walletId}")
    public ResponseEntity<Wallet> getWalletBalance(@PathVariable Long walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));
        return ResponseEntity.ok(wallet);
    }
}