package com.fyp14.OnePay.Controller;

import com.fyp14.OnePay.KeyManagement.KEK.KeyManagementService;
import com.fyp14.OnePay.Transcation.Transaction;
import com.fyp14.OnePay.Transcation.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Controller
public class AuditController {

    private final TransactionRepository transactionRepository;
    private final KeyManagementService keyManagementService;

    @Autowired
    public AuditController(TransactionRepository transactionRepository, KeyManagementService keyManagementService) {
        this.transactionRepository = transactionRepository;
        this.keyManagementService = keyManagementService;
    }

    @GetMapping("/audit")
    public String auditTransactions(Model model) {
        // Fetch all transactions ordered by transaction ID or timestamp
        List<Transaction> transactions = transactionRepository.findAllByOrderByTransactionIDAsc();

        // Prepare a list to hold the audit results
        List<String> auditResults = new ArrayList<>();

        // Initialize previousHash to the hash of the master key
        String previousHash;
        try {
            SecretKey masterKey = keyManagementService.getMasterKEKFromEnv(); // Retrieve the master key
            byte[] masterKeyBytes = masterKey.getEncoded();
            String masterKeyBase64 = Base64.getEncoder().encodeToString(masterKeyBytes);
            previousHash = computeHash(masterKeyBase64);
        } catch (Exception e) {
            // Handle exception appropriately
            model.addAttribute("error", "Failed to retrieve master key for audit: " + e.getMessage());
            return "OnePay/auditResults";  // Adjust the view name as per your setup
        }

        boolean allTransactionsValid = true;

        for (Transaction transaction : transactions) {
            StringBuilder result = new StringBuilder();

            // Verify that the previousTransactionHash matches the expected hash
            boolean isLinkValid = previousHash.equals(transaction.getPreviousTransactionHash());

            // Check if current transaction's hash was recalculated to match its stored hash
            String recalculatedHash;
            boolean isHashValid = false;
            try {
                recalculatedHash = computeTransactionHash(transaction);
                isHashValid = recalculatedHash.equals(transaction.getCurrentTransactionHash());
            } catch (Exception e) {
                recalculatedHash = "Error in hash calculation: " + e.getMessage();
                allTransactionsValid = false;
                isLinkValid = false;
            }

            // Append the audit results
            result.append("Transaction ID: ").append(transaction.getTransactionID()).append("<br>")
                    .append("Expected Previous Hash: ").append(previousHash).append("<br>")
                    .append("Actual Previous Hash: ").append(transaction.getPreviousTransactionHash()).append("<br>")
                    .append("Current Hash: ").append(transaction.getCurrentTransactionHash()).append("<br>")
                    .append("Recalculated Current Hash: ").append(recalculatedHash).append("<br>")
                    .append("Link Valid: ").append(isLinkValid ? "Yes" : "No").append("<br>")
                    .append("Hash Valid: ").append(isHashValid ? "Yes" : "No").append("<br><br>");

            auditResults.add(result.toString());

            // Set previousHash for the next transaction's verification
            if (isLinkValid && isHashValid) {
                previousHash = transaction.getCurrentTransactionHash();
            } else {
                allTransactionsValid = false;
                // Break if you want to stop auditing after finding an invalid transaction
                // break;
                // Or continue auditing all transactions
            }
        }

        // Add audit results and overall status to the model
        model.addAttribute("auditResults", auditResults);
        model.addAttribute("allTransactionsValid", allTransactionsValid);

        return "OnePay/auditResults";  // Render the dedicated auditResults.html page
    }

    // Hash computation utility for verification
    private String computeTransactionHash(Transaction transaction) throws Exception {
        String dataToHash = transaction.getPreviousTransactionHash()
                + Base64.getEncoder().encodeToString(transaction.getAmountEncrypted())
                + Base64.getEncoder().encodeToString(transaction.getIv())
                + transaction.getTransactionType().toString()
                + (transaction.getFromWallet() != null ? transaction.getFromWallet().getWalletID().toString() : "0")
                + (transaction.getToWallet() != null ? transaction.getToWallet().getWalletID().toString() : "0")
                + transaction.getTimestamp().toString()
                + (transaction.getDescription() != null ? transaction.getDescription() : "");
        return computeHash(dataToHash);
    }

    // General-purpose hash computation method
    private String computeHash(String data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
