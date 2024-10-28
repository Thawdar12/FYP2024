//for debug, to be deleted for production

package com.fyp14.OnePay.Controller;

import com.fyp14.OnePay.KeyManagement.KEK.KeyManagementService;
import com.fyp14.OnePay.KeyManagement.RSA.UserRSAKey;
import com.fyp14.OnePay.KeyManagement.RSA.UserRSAKeyRepository;
import com.fyp14.OnePay.Security.HashingService;
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
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/debug")
public class DebugController {

    private final TransactionRepository transactionRepository;
    private final KeyManagementService keyManagementService;
    private final UserRepository userRepository;
    private final HashingService hashingService;
    private final UserRSAKeyRepository userRSAKeyRepository;

    public DebugController(TransactionRepository transactionRepository, KeyManagementService keyManagementService,
                           UserRepository userRepository, HashingService hashingService,
                           UserRSAKeyRepository userRSAKeyRepository) {
        this.transactionRepository = transactionRepository;
        this.keyManagementService = keyManagementService;
        this.userRepository = userRepository;
        this.hashingService = hashingService;
        this.userRSAKeyRepository = userRSAKeyRepository;
    }

    //this endpoint to view session attributes
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

    //this endpoint for debug database record
    @GetMapping("/database")
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
                    .append("<td>").append(Base64.getEncoder().encodeToString(transaction.getAmountEncrypted())).append("</td>")
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

            byte[] encryptedAmountBytes = transaction.getAmountEncrypted();
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

    @GetMapping("/hashCheck")
    public String hashCheck() throws Exception {
        List<Transaction> transactions = transactionRepository.findAll();
        StringBuilder response = new StringBuilder();

        // Start HTML table
        response.append("<html><body><table border='1'>");
        response.append("<tr><th>User ID</th><th>Transaction ID</th><th>Hash in Database</th><th>Recalculated Hash</th><th>Same Hash</th><th>User Public Key</th><th>Transaction Signature</th><th>Signature Verified</th></tr>");

        for (Transaction transaction : transactions) {
            User user;
            String fromWalletID;
            if (transaction.getTransactionType() == TransactionType.TRANSFER) {
                user = transaction.getFromWallet().getUser();
            } else {
                user = transaction.getToWallet().getUser();
            }

            if (transaction.getFromWalletID() == null) {
                fromWalletID = "null";
            } else {
                fromWalletID = transaction.getFromWalletID().toString();
            }

            // Decrypt the user's KEK
            SecretKey userKEK = keyManagementService.decryptUserKEK(
                    user.getEncryptedKEK(),
                    user.getKekEncryptionIV(),
                    keyManagementService.getMasterKEKFromEnv()
            );

            // Decrypt the amount
            byte[] encryptedAmountBytes = transaction.getAmountEncrypted();
            String decryptedAmount = keyManagementService.decryptSensitiveData(encryptedAmountBytes, userKEK, transaction.getIv());

            // Recalculate the hash
            String recalculatedHash = hashingService.generateTransactionHash(fromWalletID, transaction.getToWalletID().toString(), decryptedAmount, transaction.getTimestamp());
            // Check if the recalculated hash matches the one in the database
            String sameHash = recalculatedHash.equals(transaction.getHashValueOfTransaction()) ? "YES" : "NO";

            // Retrieve the user's public key from the UserRSAKeyRepository
            UserRSAKey userRSAKeyEntity = userRSAKeyRepository.findByUser_UserID(user.getUserID());
            String userPublicKeyString = userRSAKeyEntity.getUserPublicKey();


            // Convert the public key from Base64 string to PublicKey object
            byte[] publicKeyBytes = Base64.getDecoder().decode(userPublicKeyString);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
            PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

            // Verify the digital signature
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(recalculatedHash.getBytes(StandardCharsets.UTF_8));
            byte[] digitalSignature = transaction.getDigitalSignature();
            boolean isSignatureValid = signature.verify(digitalSignature);
            String signatureVerificationResult = isSignatureValid ? "VALID" : "INVALID";
            String truncatedPublicKey = userPublicKeyString.length() > 20 ? userPublicKeyString.substring(0, 20) + "..." : userPublicKeyString;
            String signatureBase64 = Base64.getEncoder().encodeToString(digitalSignature);
            String truncatedSignature = signatureBase64.length() > 20 ? signatureBase64.substring(0, 20) + "..." : signatureBase64;


            // Append the data as a row in the table
            response.append("<tr>");
            response.append("<td>").append(user.getUserID()).append("</td>"); // User ID
            response.append("<td>").append(transaction.getTransactionID()).append("</td>"); // Transaction ID
            response.append("<td>").append(transaction.getHashValueOfTransaction()).append("</td>"); // Hash in Database
            response.append("<td>").append(recalculatedHash).append("</td>"); // Recalculated Hash
            response.append("<td>").append(sameHash).append("</td>"); // Same Hash Check
            response.append("<td>").append(truncatedPublicKey).append("</td>"); // User Public Key
            response.append("<td>").append(truncatedSignature).append("</td>"); // Transaction Signature (truncated)
            response.append("<td>").append(signatureVerificationResult).append("</td>"); // Signature Verification
            response.append("</tr>");
        }

        // Close the table and HTML tags
        response.append("</table></body></html>");

        return response.toString(); // Return the HTML as a string
    }



//////////////////////////////////////////////////////////////
////////// IDS with Baseline Probability and Rules ///////////
//////////////////////////////////////////////////////////////

    // Function to classify a transaction based on the probability and a fixed threshold
    private String classifyTransaction(double probability) {
        double threshold = 0.75;  // You can adjust this threshold as per your requirements
        return probability > threshold ? "Suspicious" : "Not Suspicious";
    }

    // Function to calculate the baseline probability based on the transaction amount
    private double calculateBaselineProbability(double transactionAmount) {
        if (transactionAmount < 100) {
            return 0.1;
        } else if (transactionAmount >= 100 && transactionAmount < 1000) {
            return 0.3;
        } else if (transactionAmount >= 1000 && transactionAmount <= 10000) {
            return 0.5;
        } else {
            return 0.7;  // For amounts above $10,000
        }
    }

    // Function to adjust the probability based on time of the transaction
    private double adjustProbabilityByTime(double probability, LocalTime time) {
        if (time.isAfter(LocalTime.of(7, 0)) && time.isBefore(LocalTime.of(21, 0))) {
            return probability * 1.2;  // Day time (7am-9pm)
        } else if (time.isAfter(LocalTime.of(21, 0)) && time.isBefore(LocalTime.of(0, 0))) {
            return probability * 1.5;  // Night time (9pm-12am)
        } else {
            return probability * 1.75;  // Late night (12am-7am)
        }
    }

    // Function to adjust the probability based on transaction history
    private double adjustProbabilityByHistory(double probability, double transactionAmount, List<Transaction> userTransactions, User user) {
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
        LocalDateTime twoWeeksAgo = LocalDateTime.now().minusWeeks(2);
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

        // DEBUG: Display the initial probability and transaction amount
        System.out.println(">>> Adjusting probability by history...");
        System.out.println(">>> Initial Probability: " + probability);
        System.out.println(">>> Transaction Amount: " + transactionAmount);

        // Transactions over $1000 and $10000 in the last hour
        long largeTransactionsLastHour = userTransactions.stream()
                .filter(tx -> tx.getTimestamp().isAfter(oneHourAgo) && decryptTransactionAmount(tx, user) > 1000)
                .count();
        long veryLargeTransactionsLastHour = userTransactions.stream()
                .filter(tx -> tx.getTimestamp().isAfter(oneHourAgo) && decryptTransactionAmount(tx, user) > 10000)
                .count();

        // DEBUG: Display the number of transactions over $1000 and $10000 in the last hour
        System.out.println(">>> Large Transactions Last Hour (>$1000): " + largeTransactionsLastHour);
        System.out.println(">>> Very Large Transactions Last Hour (>$10000): " + veryLargeTransactionsLastHour);

        // Adjust the probability based on recent transactions
        if (transactionAmount > 9999.99 && veryLargeTransactionsLastHour >= 1) {
            probability *= 1.6;  // If at least one transaction over $10000 in the last hour
            System.out.println(">>> Adjusted for $10,000+ transaction in last hour: " + probability);
        } else if (transactionAmount > 999.99 && largeTransactionsLastHour >= 2) {
            probability *= 1.5;  // If at least one transaction over $1000 in the last hour
            System.out.println(">>> Adjusted for $1,000+ transaction in last hour: " + probability);
        }

        // Transactions in the last week
        long largeTransactionsLastWeek = userTransactions.stream()
                .filter(tx -> tx.getTimestamp().isAfter(oneWeekAgo) && decryptTransactionAmount(tx, user) > 1000)
                .count();
        long veryLargeTransactionsLastWeek = userTransactions.stream()
                .filter(tx -> tx.getTimestamp().isAfter(oneWeekAgo) && decryptTransactionAmount(tx, user) > 10000)
                .count();

        System.out.println(">>> Large Transactions Last Week (>$1000): " + largeTransactionsLastWeek);
        System.out.println(">>> Very Large Transactions Last Week (>$10000): " + veryLargeTransactionsLastWeek);

        if (largeTransactionsLastWeek > 20) {
            probability *= 1.5;
            System.out.println(">>> Adjusted for >20 $1,000+ transactions in last week: " + probability);
        }
        if (veryLargeTransactionsLastWeek > 20) {
            probability *= 1.6;
            System.out.println(">>> Adjusted for >20 $10,000+ transactions in last week: " + probability);
        }

        // Transactions in the last two weeks
        long largeTransactionsLastTwoWeeks = userTransactions.stream()
                .filter(tx -> tx.getTimestamp().isAfter(twoWeeksAgo) && decryptTransactionAmount(tx, user) > 1000)
                .count();
        long veryLargeTransactionsLastTwoWeeks = userTransactions.stream()
                .filter(tx -> tx.getTimestamp().isAfter(twoWeeksAgo) && decryptTransactionAmount(tx, user) > 10000)
                .count();

        if (largeTransactionsLastTwoWeeks > 40) {
            probability *= 1.2;
            System.out.println(">>> Adjusted for >40 $1,000+ transactions in last two weeks: " + probability);
        }
        if (veryLargeTransactionsLastTwoWeeks > 40) {
            probability *= 1.4;
            System.out.println(">>> Adjusted for >40 $10,000+ transactions in last two weeks: " + probability);
        }

        // Large number of transactions
        if (largeTransactionsLastWeek > 100) {
            probability *= 1.1;
            System.out.println(">>> Adjusted for >100 $1,000+ transactions in last week: " + probability);
        }
        if (veryLargeTransactionsLastWeek > 100) {
            probability *= 1.2;
            System.out.println(">>> Adjusted for >100 $10,000+ transactions in last week: " + probability);
        }

        // Cap the probability at 1.0 to avoid extreme results
        probability = Math.min(probability, 1.0);

        System.out.println(">>> Final adjusted probability: " + probability);

        return probability;
    }

    // Main function that processes transactions through the IDS
    @GetMapping("/math2")
    public String compute() {
        Long userId = 3L;
        User user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            return "User not found";
        }

        // Fetch the user’s transaction history from the database
        List<Transaction> userTransactions = transactionRepository.findByUserId(user.getUserID());

        if (userTransactions.isEmpty()) {
            return "No transactions found for user.";
        }

        StringBuilder response = new StringBuilder();
        response.append("<html><body>");
        response.append("<h2>Transaction Fraud Detection Result</h2>");
        response.append("<table border='1'>");
        response.append("<tr><th>Transaction ID</th><th>Amount</th><th>Probability</th><th>Result</th></tr>");

        // Process each transaction in the user's transaction history
        for (Transaction transaction : userTransactions) {
            // Decrypt the transaction amount
            double transactionAmount = decryptTransactionAmount(transaction, user);
            System.out.println(">>> Decrypted Transaction Amount: " + transactionAmount);

            // Skip transactions with zero amount
            if (transactionAmount == 0.0) {
                System.out.println(">>> Skipping transaction due to zero amount.");
                continue;
            }

            // Initial baseline probability
            double baselineProbability = calculateBaselineProbability(transactionAmount);
            LocalTime transactionTime = transaction.getTimestamp().toLocalTime();

            // Adjust the probability based on the time of the transaction
            double adjustedProbability = adjustProbabilityByTime(baselineProbability, transactionTime);

            // Adjust the probability based on transaction history
            adjustedProbability = adjustProbabilityByHistory(adjustedProbability, transactionAmount, userTransactions, user);

            // Classify the transaction based on the final probability
            String finalResult = classifyTransaction(adjustedProbability);
            String color = finalResult.equals("Suspicious") ? "red" : "green";

            // Append the row in the HTML table with color coding and final probability
            response.append("<tr>");
            response.append("<td>").append(transaction.getTransactionID()).append("</td>");
            response.append("<td>").append(transactionAmount).append("</td>");
            response.append("<td>").append(adjustedProbability).append("</td>");
            response.append("<td style='color:").append(color).append(";'>").append(finalResult).append("</td>");
            response.append("</tr>");
        }

        response.append("</table>");
        response.append("</body></html>");
        return response.toString();
    }

    // Helper method to decrypt transaction amount
    private double decryptTransactionAmount(Transaction transaction, User user) {
        double amount = 0;
        if (user != null && user.getEncryptedKEK() != null && user.getKekEncryptionIV() != null) {
            try {
                SecretKey userKEK = keyManagementService.decryptUserKEK(
                        user.getEncryptedKEK(),
                        user.getKekEncryptionIV(),
                        keyManagementService.getMasterKEKFromEnv()
                );
                byte[] encryptedAmountBytes = transaction.getAmountEncrypted();
                String decryptedAmount = keyManagementService.decryptSensitiveData(encryptedAmountBytes, userKEK, transaction.getIv());
                if (decryptedAmount != null) {
                    amount = Double.parseDouble(decryptedAmount);
                    System.out.println(">>> Successfully decrypted amount: " + amount);
                } else {
                    System.out.println(">>> Decrypted amount is null for transaction ID: " + transaction.getTransactionID());
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(">>> Exception in decryption for transaction ID: " + transaction.getTransactionID());
            }
        } else {
            System.out.println(">>> KEK or IV is missing for decryption.");
        }
        return amount;
    }

    // Helper method to calculate transaction frequency
    private double calculateTransactionFrequency(User user, List<Transaction> transactions) {
        long daysSinceAccountCreation = Duration.between(user.getCreated_at(), LocalDateTime.now()).toDays() + 1;  // Avoid division by zero
        return (double) transactions.size() / daysSinceAccountCreation;
    }

}