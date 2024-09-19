//key management system
//

package com.fyp14.OnePay.Security;

import com.fyp14.OnePay.User.User;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class KeyManagementService {
    private static final int GCM_IV_LENGTH = 12;  // 12-byte IV for GCM mode
    private static final int GCM_TAG_LENGTH = 128; // 128-bit authentication tag

    // Generate a new Master KEK (to be done once)
    public String generateMasterKEK() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256); // AES-256 for strong encryption
        SecretKey masterKEK = keyGen.generateKey();

        // Return the Base64 encoded string of the key for storage
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
        return cipher.doFinal(data.getBytes("UTF-8")); // Encrypt the data
    }

    // Method used for decryption of data, only accept string
    public String decryptSensitiveData(byte[] encryptedData, SecretKey userKEK, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, userKEK, gcmSpec);
        byte[] decryptedDataBytes = cipher.doFinal(encryptedData);
        return new String(decryptedDataBytes, "UTF-8"); // Convert decrypted data back to string
    }
}
