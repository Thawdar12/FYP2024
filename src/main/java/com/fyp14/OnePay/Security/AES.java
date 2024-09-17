package com.fyp14.OnePay.Security;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

// public class containing encryption/decryption processes
public class AES {

    private static SecretKey secretKey; // Holds the dynamically generated key
    private static final String INIT_VECTOR = "encryptionIntVec"; // Must be 16 bytes for AES

    // generates random 128-bit key value
    public static void generateKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128); // Set key size to 128 bits
        secretKey = keyGen.generateKey(); // Generate the key and stores it in variable
    }

    // encrypts string data
    public static String encrypt(String dataToEncrypt) {
        try {

            // protects data by using unique ciphertext message
            IvParameterSpec ivspec = new IvParameterSpec(INIT_VECTOR.getBytes(StandardCharsets.UTF_8));

            // Specifies the encryption algorithm (AES)
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

            // encrypts with cipher, secret key, and IV spec
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivspec);
            return Base64.getEncoder().encodeToString(cipher.doFinal(dataToEncrypt.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            System.out.println("Error while encrypting: " + e.toString());
        }
        return null; // returns null if error
    }

    //decrypts string data
    public static String decrypt(String encryptedData) {
        try {
            // protects data by using unique ciphertext message
            IvParameterSpec ivspec = new IvParameterSpec(INIT_VECTOR.getBytes(StandardCharsets.UTF_8));

            // Specifies the encryption algorithm (AES)
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");

            // decrypts with cipher, secret key, and IV spec
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivspec);
            return new String(cipher.doFinal(Base64.getDecoder().decode(encryptedData)));
        } catch (Exception e) {
            System.out.println("Error while decrypting: " + e.toString());
        }
        return null; // returns null if error
    }

    // Separates data values from URL using map interface
    public static Map<String, String> getQueryParams(String url) throws Exception {
        Map<String, String> queryPairs = new HashMap<>();
        URI uri = new URI(url);
        String[] pairs = uri.getQuery().split("&"); //splits the URL containing '&'
        for (String pair : pairs) {
            String[] keyValue = pair.split("="); // finds key values and splits them according to '='
            queryPairs.put(keyValue[0], keyValue[1]); // splits into mapkey and mapvalue
        }
        return queryPairs;
    }

    //Stores data for TransactionID, Amount and ReceiverName
    public static void storeDataInDatabase(String encryptedTransactionID, String encryptedAmount, String encryptedReceiverName) throws Exception {
        // database details
        String url = "jdbc:mariadb://localhost:3306/FYPDatabase?createDatabaseIfNotExist=true";
        String user = "root";
        String password = "@Anv6rsrwcc";
        Connection conn = DriverManager.getConnection(url, user, password);

        // database value type for all must be varchar to store
        String sql = "INSERT INTO transaction (TransactionID, amount, receiverName) VALUES (?, ?, ?)";
        PreparedStatement pstmt = conn.prepareStatement(sql);

        // sets data value into strings
        pstmt.setString(1, encryptedTransactionID);
        pstmt.setString(2, encryptedAmount);
        pstmt.setString(3, encryptedReceiverName);

        // Executes SQL statement
        pstmt.executeUpdate();
    }

    // retrieves data from the database by TransactionID (checks if matches)
    public static String retrieveFromDatabase(String transactionID) {
        String rebuiltUrl = null;  // Initialize the URL string
        try {
            // Encrypt the provided TransactionID to match the stored encrypted value
            String encryptedTransactionID = encrypt(transactionID);

            // database details
            String url = "jdbc:mariadb://localhost:3306/FYPDatabase?createDatabaseIfNotExist=true";
            String user = "root";
            String password = "@Anv6rsrwcc";
            Connection conn = DriverManager.getConnection(url, user, password);

            // Use the encrypted TransactionID in the query
            String sql = "SELECT TransactionID, amount, receiverName FROM transaction WHERE TransactionID = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, encryptedTransactionID);  // Use encrypted TransactionID here
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {

                // gets values base on their respective keys
                String dbEncryptedTransactionID = rs.getString("TransactionID");
                String encryptedAmount = rs.getString("amount");
                String encryptedReceiverName = rs.getString("receiverName");

                // Decrypt the TransactionID from the database
                String dbTransactionID = decrypt(dbEncryptedTransactionID);

                // Check if the decrypted TransactionID matches the original parameter
                if (transactionID.equals(dbTransactionID)) {
                    // Decrypt the other fields
                    String amount = decrypt(encryptedAmount);
                    String receiverName = decrypt(encryptedReceiverName);

                    // Rebuild the URL with the decrypted values (change URL address)
                    rebuiltUrl = "https://example.com?TransactionID=" + transactionID + "&amount=" + amount
                            + "&receiverName=" + receiverName;
                } else {
                    System.out.println("TransactionID mismatch.");
                }
            } else {
                System.out.println("TransactionID not found.");
            }
        } catch (Exception e) {
            System.out.println("Error while retrieving or decrypting data: " + e.toString());
        }

        return rebuiltUrl;  // Return the reconstructed URL
    }

}
