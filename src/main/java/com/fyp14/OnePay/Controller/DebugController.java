//for debug, to be deleted for production

package com.fyp14.OnePay.Controller;

import com.fyp14.OnePay.Security.KeyManagementService;
import com.fyp14.OnePay.Transcation.Transaction;
import com.fyp14.OnePay.Transcation.TransactionRepository;
import com.fyp14.OnePay.User.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/debug")
public class DebugController {

    private final TransactionRepository transactionRepository;
    private final KeyManagementService keyManagementService;

    public DebugController(TransactionRepository transactionRepository, KeyManagementService keyManagementService) {
        this.transactionRepository = transactionRepository;
        this.keyManagementService = keyManagementService;
    }

    // Debug endpoint to view session attributes
    @GetMapping("/session")
    public Map<String, Object> getSessionAttributes(HttpSession session) {
        Map<String, Object> sessionData = new HashMap<>();
        Enumeration<String> attributeNames = session.getAttributeNames();
        while (attributeNames.hasMoreElements()) {
            String attributeName = attributeNames.nextElement();
            sessionData.put(attributeName, session.getAttribute(attributeName));
        }
        return sessionData;
    }

    @GetMapping("/amount")
    public String getTransactionAmount() throws Exception {
        // Step 1: Fetch the transaction with transactionID = 2
        Transaction transaction = transactionRepository.findById(3L)
                .orElseThrow(() -> new Exception("Transaction not found"));

        // Step 2: Fetch the associated user and their KEK
        User user = transaction.getToWallet().getUser();
        SecretKey userKEK = keyManagementService.decryptUserKEK(user.getEncryptedKEK(), user.getKekEncryptionIV(), keyManagementService.getMasterKEKFromEnv());

        // Step 3: Decode the Base64 encrypted amount from the database
        byte[] encryptedAmountBytes = Base64.getDecoder().decode(transaction.getAmountEncrypted());

        // Step 4: Decrypt the amount using the user's KEK and the IV
        String decryptedAmount = keyManagementService.decryptSensitiveData(encryptedAmountBytes, userKEK, transaction.getIv());

        // Return the decrypted amount as plain text
        return "Decrypted amount for transaction ID 2: " + decryptedAmount;
    }
}
