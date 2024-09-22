//key management system
//MASTER KEK and User KEK is generate here

package com.fyp14.OnePay.KeyManagement.KEK;

import com.fyp14.OnePay.Transcation.Transaction;
import com.fyp14.OnePay.Transcation.TransactionRepository;
import com.fyp14.OnePay.Transcation.TransactionType;
import com.fyp14.OnePay.User.User;
import com.fyp14.OnePay.User.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class KeyManagementService {
    private static final int GCM_IV_LENGTH = 12;  // 12-byte IV for GCM mode
    private static final int GCM_TAG_LENGTH = 128; // 128-bit authentication tag

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public KeyManagementService(TransactionRepository transactionRepository, UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    // Generate a new Master KEK (to be done once)
    public String generateMasterKEK() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256); // AES-256 for strong encryption
        SecretKey masterKEK = keyGen.generateKey();
        return Base64.getEncoder().encodeToString(masterKEK.getEncoded());
    }

    // Retrieve the Master KEK from the environment variable
    public SecretKey getMasterKEKFromEnv() {
        String base64Key = System.getenv("MASTER_KEK"); // Read from environment variable
        if (base64Key == null || base64Key.isEmpty()) {
            throw new RuntimeException("Master KEK not found in environment variables");
        }
        byte[] decodedKey = Base64.getDecoder().decode(base64Key); // Decode the Base64 string
        return new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES"); // Rebuild the key
    }

    // Encrypt the user's KEK with the Master KEK
    public byte[] encryptUserKEK(SecretKey userKEK, SecretKey masterKEK, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, masterKEK, gcmSpec);
        return cipher.doFinal(userKEK.getEncoded());
    }

    // Decrypt the user's KEK using the Master KEK and the stored IV
    public SecretKey decryptUserKEK(byte[] encryptedKEK, byte[] iv, SecretKey masterKEK) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, masterKEK, gcmSpec);
        byte[] decryptedKEKBytes = cipher.doFinal(encryptedKEK);
        return new SecretKeySpec(decryptedKEKBytes, 0, decryptedKEKBytes.length, "AES"); // Return the decrypted user KEK
    }

    // Generate a random IV for GCM mode
    public byte[] generateRandomIV() {
        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        return iv;
    }

    // Generate a unique KEK for each user (AES-256)
    public SecretKey generateUserKEK() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256); // AES-256 for strong encryption
        return keyGen.generateKey(); // This is the user's KEK
    }

    // Generate and encrypt the user's KEK
    public void processUserKEK(User user) throws Exception {
        // Step 1: Generate a unique KEK for the user
        SecretKey userKEK = generateUserKEK();

        // Step 2: Retrieve the Master KEK from the environment variable
        SecretKey masterKEK = getMasterKEKFromEnv();

        // Step 3: Generate a random IV for encryption
        byte[] iv = generateRandomIV();

        // Step 4: Encrypt the user's KEK with the Master KEK
        byte[] encryptedKEK = encryptUserKEK(userKEK, masterKEK, iv);

        // Step 5: Store the encrypted KEK and IV in the User entity
        user.setEncryptedKEK(encryptedKEK);
        user.setKekEncryptionIV(iv);
    }

    // Method used for encryption of data, only accept string
    public byte[] encryptSensitiveData(String data, SecretKey userKEK, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, userKEK, gcmSpec);
        return cipher.doFinal(data.getBytes(StandardCharsets.UTF_8)); // Encrypt the data
    }

    // Method used for decryption of data, only accept string
    public String decryptSensitiveData(byte[] encryptedData, SecretKey userKEK, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, userKEK, gcmSpec);
        byte[] decryptedDataBytes = cipher.doFinal(encryptedData);
        return new String(decryptedDataBytes, StandardCharsets.UTF_8); // Convert decrypted data back to string
    }

    //TODO:remember to rotate RSA Keys as well
    @Transactional
    public void rotateMasterKEK() throws Exception {
        // Step 1: Retrieve the current Master KEK from the environment
        SecretKey currentMasterKEK = getMasterKEKFromEnv();

        // Step 2: Retrieve all users from the database
        List<User> allUsers = userRepository.findAll();

        // Step 3: Create a list to store user key data
        List<UserKeyData> userKeyDataList = new ArrayList<>();

        // Step 4: Loop through users and decrypt their KEK using the current Master KEK
        for (User user : allUsers) {
            // Retrieve the encrypted KEK, private key, and IV for each user
            byte[] encryptedKEK = user.getEncryptedKEK();
            byte[] kekIV = user.getKekEncryptionIV();
            byte[] encryptedPrivateKey = user.getEncryptedPrivateKey();

            // Decrypt the user's KEK using the current Master KEK
            SecretKey decryptedUserKEK = decryptUserKEK(encryptedKEK, kekIV, currentMasterKEK);

            // Decrypt the user's private key using the decrypted user KEK and the same IV (kekIV)
            String decryptedPrivateKeyBase64 = decryptSensitiveData(encryptedPrivateKey, decryptedUserKEK, kekIV);
            byte[] decryptedPrivateKeyBytes = Base64.getDecoder().decode(decryptedPrivateKeyBase64);

            // Create a UserKeyData object to store the userID, KEK, and private key
            userKeyDataList.add(new UserKeyData(user.getUserID(), decryptedUserKEK, decryptedPrivateKeyBytes));
        }

        // Step 5: Generate a new Master KEK
        String base64NewMasterKEK = generateMasterKEK();
        byte[] decodedNewMasterKEK = Base64.getDecoder().decode(base64NewMasterKEK);
        SecretKey newMasterKEK = new SecretKeySpec(decodedNewMasterKEK, 0, decodedNewMasterKEK.length, "AES");
        System.out.println("\nTHIS IS THE NEW MASTER KEY: " + base64NewMasterKEK + "\nPLEASE STORE INTO ENV VARIABLE\n");

        // Step 6: Loop through the list of user KEKs and re-encrypt them with the new Master KEK
        for (UserKeyData userData : userKeyDataList) {
            Long userID = userData.getUserID();
            SecretKey userKEK = userData.getUserKEK();
            byte[] privateKeyBytes = userData.getPrivateKey();

            // Generate a new IV for encrypting the KEK and private key
            byte[] newIV = generateRandomIV();

            // Encrypt the user's KEK with the new Master KEK
            byte[] encryptedKEK = encryptUserKEK(userKEK, newMasterKEK, newIV);

            // Encrypt the user's private key with the new User KEK using the same new IV
            String privateKeyBase64 = Base64.getEncoder().encodeToString(privateKeyBytes); // Encode the private key as Base64 string
            byte[] encryptedPrivateKey = encryptSensitiveData(privateKeyBase64, userKEK, newIV); // Encrypt the private key

            // Update the user in the database with the new encrypted KEK, IV, and private key
            User user = userRepository.findById(userID).orElseThrow(() -> new Exception("User not found"));
            user.setEncryptedKEK(encryptedKEK);
            user.setKekEncryptionIV(newIV);
            user.setEncryptedPrivateKey(encryptedPrivateKey);

            // Save the updated user back to the database
            userRepository.save(user);
        }
        // At this point, all users' KEKs and private key have been rotated with the new Master KEK
        System.out.println("\nAll users' KEKs have been rotated with the new Master KEK, proceed with re-encrypt transaction records\n");

        // Next we change record in transactions table
        // Step 7: Retrieve all transactions from the Transaction table
        List<Transaction> allTransactions = transactionRepository.findAll();

        // Step 8: Loop through each transaction and process the decryption and re-encryption
        for (Transaction transaction : allTransactions) {
            Long walletId = null;
            SecretKey userKEK = null;

            // Step 9: Determine the wallet ID based on the transaction type
            if (transaction.getTransactionType() == TransactionType.TRANSFER) {
                // Always use from_wallet_id for TRANSFER
                walletId = transaction.getFromWallet() != null ? transaction.getFromWallet().getWalletID() : null;
            } else if (transaction.getTransactionType() == TransactionType.DEPOSIT) {
                // Always use to_wallet_id for DEPOSIT
                walletId = transaction.getToWallet() != null ? transaction.getToWallet().getWalletID() : null;
            }
            if (walletId == null) {
                throw new Exception("Wallet ID not found for transaction: " + transaction.getTransactionID());
            }

            // Step 10: Find the corresponding User KEK by matching wallet ID with the UserKeyData list
            User correspondingUser = userRepository.findByWalletId(walletId);
            if (correspondingUser == null) {
                throw new Exception("No corresponding user found for wallet ID: " + walletId);
            }

            // Loop through the list of UserKeyData objects to find the matching userID
            for (UserKeyData userData : userKeyDataList) {
                if (userData.getUserID().equals(correspondingUser.getUserID())) {
                    userKEK = userData.getUserKEK(); // Found the User KEK corresponding to the wallet
                    break;
                }
            }

            if (userKEK == null) {
                throw new Exception("No corresponding user KEK found for wallet ID: " + walletId);
            }

            // Step 11: Decrypt the old transaction amount using the old User KEK
            byte[] oldEncryptedAmount = transaction.getAmountEncrypted();
            String decryptedAmount = decryptSensitiveData(oldEncryptedAmount, userKEK, transaction.getIv()); // Use the plain user KEK from the userKEKList

            // Step 12: Generate a new IV for the re-encryption
            byte[] newIv = generateRandomIV();

            // Step 13: Re-encrypt the decrypted amount using the updated User KEK
            SecretKey updatedUserKEK = decryptUserKEK(userRepository.findByWalletId(walletId).getEncryptedKEK(),
                    userRepository.findByWalletId(walletId).getKekEncryptionIV(),
                    newMasterKEK);// Use the new Master KEK to decrypt the updated User KEK

            byte[] newEncryptedAmountBytes = encryptSensitiveData(decryptedAmount, updatedUserKEK, newIv); // Use the new User KEK decrypted with the new Master KEK

            // Step 14: Encode the new encrypted amount and update the transaction
            transaction.setAmountEncrypted(newEncryptedAmountBytes);
            transaction.setIv(newIv); // Update with the new IV

            // Step 15: Save the updated transaction back to the database
            transactionRepository.save(transaction);
        }
        System.out.println("\nAll transaction records details have been encrypted with NEW Master KEK, Key rotation Completed\n");
    }
}
