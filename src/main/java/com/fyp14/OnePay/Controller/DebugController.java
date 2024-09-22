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
import java.util.*;

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
}
