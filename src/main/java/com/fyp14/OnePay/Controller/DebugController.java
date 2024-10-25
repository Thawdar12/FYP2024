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
////////////////// Testing Phase for IDS /////////////////////
//////////////////////////////////////////////////////////////

    // Define base intercept as a constant or class-level variable
    private final double baseIntercept = -10.0;

    // Function to calculate risk score with low transaction detection
    private double calculateSuspiciousProbabilityWithLowTransaction(double transactionAmount, double avgAmount, double frequency) {
        double lowAmountThreshold = avgAmount * 0.01;  // 10% of the average as a baseline for low transactions
        boolean isLowTransaction = transactionAmount < lowAmountThreshold;

        double weightAmount = calculateDynamicWeightAmount(transactionAmount, avgAmount, isLowTransaction);
        double weightFrequency = calculateDynamicWeightFrequency(frequency);

        // Calculate linear combination (z)
        double z = baseIntercept + (weightAmount * transactionAmount) + (weightFrequency * frequency);

        // Apply sigmoid function
        return sigmoid(z);
    }

    // Calculate dynamic weight based on low/high transaction
    private double calculateDynamicWeightAmount(double transactionAmount, double avgAmount, boolean isLowTransaction) {
        if (isLowTransaction) {
            return 0.3;  // Assign a higher weight for very low transactions
        }
        return 0.1 * (1 + Math.log1p(Math.abs(transactionAmount - avgAmount) / avgAmount));  // Original deviation-based weight
    }

    // Step 1: Calculate dynamic threshold based on user behavior, transaction amount, and specific transaction
    private double calculateDynamicThreshold(User user, double transactionAmount, List<Transaction> userTransactions, Transaction transaction) {
        double avgAmount = calculateAverageAmount(userTransactions, user);  // Use user's average amount
        double frequency = calculateTransactionFrequency(user, userTransactions);  // Calculate frequency for this user
        double movingAvg = calculateMovingAverage(userTransactions, user);  // Calculate moving average for evolving patterns

        // Case 1: Large vs. Small Transaction to a new contact
        if (isNewContact(transaction) && transactionAmount < (avgAmount * 0.1)) {
            return 0.7;  // Flag smaller transactions made to new contacts
        }

        // Case 2: Low-value scam detection (transaction below $100 and at suspicious time)
        if (transactionAmount < 100 && isSuspiciousTime(transaction)) {
            return 0.6;  // Flag smaller transactions made at suspicious times
        }

        // Case 3: Evolving spending patterns (dynamic adjustment)
        if (transactionAmount > movingAvg * 2) {
            return 0.9;  // Flag transactions that are twice the user's current moving average
        }

        // Default logic for dynamic threshold
        if (avgAmount > 5000 && transactionAmount > 10000) {
            return 0.8;  // Higher threshold for larger transactions
        } else if (transactionAmount < 100) {
            return 0.3;  // Lower threshold for small transactions
        } else if (frequency < 5) {
            return 0.4;  // Lower threshold for infrequent transactions
        } else {
            return 0.5;  // Default threshold
        }
    }

    // Helper method to calculate the moving average for evolving spending patterns
    private double calculateMovingAverage(List<Transaction> transactions, User user) {
        int windowSize = Math.min(30, transactions.size());  // Define a time window for recent transactions
        return transactions.stream()
                .skip(transactions.size() - windowSize)  // Focus on recent transactions
                .mapToDouble(transaction -> decryptTransactionAmount(transaction, user))  // Decrypt amounts using the user
                .average()
                .orElse(0.0);
    }

    // Helper method to check if the transaction is made to a new contact
    private boolean isNewContact(Transaction transaction) {
        // Logic to check if the recipient is new (e.g., check user’s transaction history)
        return false;  // Placeholder
    }

    // Helper method to check for suspicious transaction time (late night or unusual hours)
    private boolean isSuspiciousTime(Transaction transaction) {
        LocalTime time = transaction.getTimestamp().toLocalTime();
        return time.isAfter(LocalTime.of(22, 0)) || time.isBefore(LocalTime.of(6, 0));  // Late-night transactions
    }

    // Function to calculate user risk score and adjust for increasing spending trends over time
    private double calculateUserRiskScore(User user) {
        List<Transaction> userTransactions = transactionRepository.findByUserId(user.getUserID());
        if (userTransactions.isEmpty()) {
            return 0.0;  // No transactions, neutral risk
        }

        // Calculate user's time-based spending pattern (average over the past 6 months)
        double avgAmount = calculateAverageAmount(userTransactions, user);
        double riskScore = 0.0;
        for (Transaction transaction : userTransactions) {
            double transactionAmount = decryptTransactionAmount(transaction, user);
            double frequency = calculateTransactionFrequency(user, userTransactions);
            double suspiciousProbability = calculateSuspiciousProbabilityWithLowTransaction(transactionAmount, avgAmount, frequency);

            riskScore += suspiciousProbability;
        }

        // Calculate average score
        return riskScore / userTransactions.size();
    }

    // Main endpoint to test and display results
    @GetMapping("/math2")
    public String compute2() {
        // Fetch the user (using 3L as the user ID)
        Long someUserId = 3L;  // Replace this with logic to fetch the correct user ID
        User user = userRepository.findById(someUserId).orElse(null);

        if (user == null) {
            return "User not found";
        }

        // Fetch all transactions for the user
        List<Transaction> userTransactions = transactionRepository.findByUserId(user.getUserID());

        if (userTransactions.isEmpty()) {
            return "No transactions found for user.";
        }

        // Now call the calculateMovingAverage method with both arguments
        double movingAvg = calculateMovingAverage(userTransactions, user);  // Ensure you pass both userTransactions and user
        double frequency = calculateTransactionFrequency(user, userTransactions);


        // Build HTML response
        StringBuilder response = new StringBuilder();
        response.append("<html><body>");
        response.append("<h2>User Transaction Suspicious Activity Probability</h2>");
        response.append("<table border='1'>");
        response.append("<tr><th>Transaction ID</th><th>Amount</th><th>Suspicious Probability</th><th>Classification</th></tr>");

        // Calculate avgAmount before using it
        double avgAmount = calculateAverageAmount(userTransactions, user);  // Calculate the average amount

        // Now process each transaction
        for (Transaction transaction : userTransactions) {
            double decryptedAmount = decryptTransactionAmount(transaction, user);
            double deviationAmount = calculateDeviation(decryptedAmount, avgAmount);
            double suspiciousProbability = calculateSuspiciousProbabilityWithLowTransaction(decryptedAmount, avgAmount, frequency);
            double threshold = calculateDynamicThreshold(user, decryptedAmount, userTransactions, transaction);
            String classification = classifySuspicion(suspiciousProbability, threshold);

            String color = classification.equals("Suspicious") ? "red" : "green";
            response.append("<tr>")
                    .append("<td>").append(transaction.getTransactionID()).append("</td>")
                    .append("<td>").append(decryptedAmount).append("</td>")
                    .append("<td>").append(suspiciousProbability).append("</td>")
                    .append("<td style='color:").append(color).append(";'>").append(classification).append("</td>")
                    .append("</tr>");
        }

        double userRiskScore = calculateUserRiskScore(user);
        response.append("</table>");
        response.append("<h3>General User Risk Score: ").append(userRiskScore).append("</h3>");
        response.append("</body></html>");

        return response.toString();
    }


    // Helper method to decrypt transaction amount
    private double decryptTransactionAmount(Transaction transaction, User user) {
        double amount = 0;
        if (user != null && user.getEncryptedKEK() != null && user.getKekEncryptionIV() != null) {
            try {
                // Decrypt the user's KEK to get the SecretKey
                SecretKey userKEK = keyManagementService.decryptUserKEK(
                        user.getEncryptedKEK(),
                        user.getKekEncryptionIV(),
                        keyManagementService.getMasterKEKFromEnv()
                );

                // Decrypt the transaction amount using the decrypted KEK
                byte[] encryptedAmountBytes = transaction.getAmountEncrypted();
                String decryptedAmount = keyManagementService.decryptSensitiveData(encryptedAmountBytes, userKEK, transaction.getIv());

                // Convert decrypted string to double
                if (decryptedAmount != null) {
                    amount = Double.parseDouble(decryptedAmount);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return amount;
    }

    // Helper method to calculate average transaction amount
    private double calculateAverageAmount(List<Transaction> transactions, User user) {
        return transactions.stream()
                .mapToDouble(transaction -> decryptTransactionAmount(transaction, user))  // Decrypt the amount for each transaction
                .average()
                .orElse(0.0);
    }

    // Helper method to calculate transaction frequency
    private double calculateTransactionFrequency(User user, List<Transaction> transactions) {
        long daysSinceAccountCreation = Duration.between(user.getCreated_at(), LocalDateTime.now()).toDays() + 1;  // To avoid division by zero
        return (double) transactions.size() / daysSinceAccountCreation;
    }

    // Helper method to calculate the deviation from the average amount
    private double calculateDeviation(double transactionAmount, double avgAmount) {
        return Math.abs(transactionAmount - avgAmount) / avgAmount;
    }

    // Dynamic logistic regression calculation for suspicious activity probability
    private double calculateSuspiciousProbability(double deviationAmount, double frequency) {
        // Dynamically adjust weights based on deviation and frequency
        double weightAmount = calculateDynamicWeightAmount(deviationAmount);
        double weightFrequency = calculateDynamicWeightFrequency(frequency);

        // Calculate linear combination (z)
        double z = baseIntercept + (weightAmount * deviationAmount) + (weightFrequency * frequency);

        // Apply sigmoid function
        return sigmoid(z);
    }

    // Helper methods to calculate dynamic weights based on user behavior
    private double calculateDynamicWeightAmount(double deviationAmount) {
        return 0.1 * (1 + Math.log1p(deviationAmount));  // Use log1p for smoother scaling of large values
    }

    private double calculateDynamicWeightFrequency(double frequency) {
        return 0.2 * (1 + Math.log1p(frequency));  // Logarithmic scaling for frequency
    }

    // Sigmoid function for logistic regression
    private double sigmoid(double z) {
        return 1.0 / (1.0 + Math.exp(-z));
    }



    // Step 2: Classify transaction as suspicious or not based on the probability and threshold
    private String classifySuspicion(double suspiciousProbability, double threshold) {
        if (suspiciousProbability >= threshold) {
            return "Suspicious";
        } else {
            return "Not Suspicious";
        }
    }
}