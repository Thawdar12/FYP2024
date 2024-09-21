//for debug, to be deleted for production

package com.fyp14.OnePay.Controller;

import com.fyp14.OnePay.Security.KeyManagementService;
import com.fyp14.OnePay.Transcation.Transaction;
import com.fyp14.OnePay.Transcation.TransactionRepository;
import com.fyp14.OnePay.Transcation.TransactionType;
import com.fyp14.OnePay.User.User;
import com.fyp14.OnePay.User.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.SecretKey;
import java.util.*;

@RestController
@RequestMapping("/debug")
public class DebugController {

    private final TransactionRepository transactionRepository;
    private final KeyManagementService keyManagementService;
    private final UserRepository userRepository;

    public DebugController(TransactionRepository transactionRepository, KeyManagementService keyManagementService, UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.keyManagementService = keyManagementService;
        this.userRepository = userRepository;
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
    @ResponseBody
    public String displayData() throws Exception {
        StringBuilder response = new StringBuilder();

        // Start HTML
        response.append("<html><body>");

        // Step 1: Display raw user data in a table
        response.append("<h2>User Table - Raw Data</h2>");
        response.append("<table border='1'>")
                .append("<tr><th>User ID</th><th>Username</th><th>Created At</th><th>Email</th><th>Enabled?</th>")
                .append("<th>Locked?</th><th>Password</th><th>Phone Number</th><th>Wallet ID</th><th>Encrypted KEK</th><th>KEK IV</th></tr>");

        List<User> users = userRepository.findAll();
        for (User user : users) {
            response.append("<tr>")
                    .append("<td>").append(user.getUserID()).append("</td>")
                    .append("<td>").append(user.getUsername()).append("</td>")
                    .append("<td>").append(user.getCreated_at()).append("</td>")
                    .append("<td>").append(user.getEmail()).append("</td>")
                    .append("<td>").append(user.getEnabled()).append("</td>")
                    .append("<td>").append(user.getLocked()).append("</td>")
                    .append("<td>").append(user.getPassword()).append("</td>")
                    .append("<td>").append(user.getPhoneNumber()).append("</td>")
                    .append("<td>").append(user.getWalletID()).append("</td>")
                    .append("<td>").append(Base64.getEncoder().encodeToString(user.getEncryptedKEK())).append("</td>")
                    .append("<td>").append(Base64.getEncoder().encodeToString(user.getKekEncryptionIV())).append("</td>")
                    .append("</tr>");
        }
        response.append("</table>");

        // Step 2: Display decrypted user data in a table
        response.append("<h2>User Table - Decrypted Data</h2>");
        response.append("<table border='1'>")
                .append("<tr><th>User ID</th><th>Username</th><th>Decrypted KEK</th></tr>");

        for (User user : users) {
            SecretKey decryptedKEK = keyManagementService.decryptUserKEK(
                    user.getEncryptedKEK(),
                    user.getKekEncryptionIV(),
                    keyManagementService.getMasterKEKFromEnv()
            );

            response.append("<tr>")
                    .append("<td>").append(user.getUserID()).append("</td>")
                    .append("<td>").append(user.getUsername()).append("</td>")
                    .append("<td>").append(Base64.getEncoder().encodeToString(decryptedKEK.getEncoded())).append("</td>")
                    .append("</tr>");
        }
        response.append("</table>");

        // Step 3: Display raw transaction data in a table
        response.append("<h2>Transaction Table - Raw Data</h2>");
        response.append("<table border='1'>")
                .append("<tr><th>Transaction ID</th><th>Amount (Encrypted)</th><th>Description</th><th>IV</th>")
                .append("<th>Transaction Type</th><th>Status</th><th>Timestamp</th><th>From Wallet ID</th><th>To Wallet ID</th></tr>");

        List<Transaction> transactions = transactionRepository.findAll();
        for (Transaction transaction : transactions) {
            response.append("<tr>")
                    .append("<td>").append(transaction.getTransactionID()).append("</td>")
                    .append("<td>").append(transaction.getAmountEncrypted()).append("</td>")
                    .append("<td>").append(transaction.getDescription()).append("</td>")
                    .append("<td>").append(Base64.getEncoder().encodeToString(transaction.getIv())).append("</td>")
                    .append("<td>").append(transaction.getTransactionType()).append("</td>")
                    .append("<td>").append(transaction.getStatus()).append("</td>")
                    .append("<td>").append(transaction.getTimestamp()).append("</td>")
                    .append("<td>").append(transaction.getFromWallet() == null ? "null" : transaction.getFromWalletID().toString()).append("</td>")
                    .append("<td>").append(transaction.getToWallet() == null ? "null" : transaction.getToWalletID().toString()).append("</td>")
                    .append("</tr>");
        }
        response.append("</table>");

        // Step 4: Display decrypted transaction data in a table
        response.append("<h2>Transaction Table - Decrypted Data</h2>");
        response.append("<table border='1'>")
                .append("<tr><th>Transaction ID</th><th>Decrypted Amount</th><th>Transaction Type</th></tr>");

        for (Transaction transaction : transactions) {
            User user;
            if (transaction.getTransactionType() == TransactionType.TRANSFER) {
                user = transaction.getFromWallet().getUser();
            } else {
                user = transaction.getToWallet().getUser();
            }

            SecretKey userKEK = keyManagementService.decryptUserKEK(
                    user.getEncryptedKEK(),
                    user.getKekEncryptionIV(),
                    keyManagementService.getMasterKEKFromEnv()
            );

            byte[] encryptedAmountBytes = Base64.getDecoder().decode(transaction.getAmountEncrypted());
            String decryptedAmount = keyManagementService.decryptSensitiveData(encryptedAmountBytes, userKEK, transaction.getIv());

            response.append("<tr>")
                    .append("<td>").append(transaction.getTransactionID()).append("</td>")
                    .append("<td>").append(decryptedAmount).append("</td>")
                    .append("<td>").append(transaction.getTransactionType()).append("</td>")
                    .append("</tr>");
        }
        response.append("</table>");

        // End HTML
        response.append("</body></html>");

        return response.toString();
    }

}
