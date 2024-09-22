package com.fyp14.OnePay.KeyManagement.RSA;

import com.fyp14.OnePay.KeyManagement.KEK.KeyManagementService;
import com.fyp14.OnePay.User.User;
import com.fyp14.OnePay.User.UserRepository;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
public class UserRSAKeyService {
    private final UserRSAKeyRepository userRSAKeyRepository;
    private final KeyManagementService keyManagementService;
    private final UserRepository userRepository;

    public UserRSAKeyService(UserRSAKeyRepository userRSAKeyRepository, KeyManagementService keyManagementService
            , UserRepository userRepository) {
        this.userRSAKeyRepository = userRSAKeyRepository;
        this.keyManagementService = keyManagementService;
        this.userRepository = userRepository;
    }

    public UserRSAKey getUserPublicKey(Long userId) {
        return userRSAKeyRepository.findByUser_UserID(userId);
    }

    public void generateAndStoreKeyPairs(User user) throws Exception {
        // Generate RSA key pair
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        // Get the public key and encode it to Base64
        String publicKeyEncoded = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

        // Store the public key in the database
        UserRSAKey userPublicKey = new UserRSAKey();
        userPublicKey.setUser(user);  // Set the User entity directly
        userPublicKey.setUserPublicKey(publicKeyEncoded);
        userPublicKey.setCreatedAt(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
        userRSAKeyRepository.save(userPublicKey);

        // Get the private key as a byte array
        byte[] privateKey = keyPair.getPrivate().getEncoded();

        // Encrypt the private key
        SecretKey masterKEK = keyManagementService.getMasterKEKFromEnv();
        SecretKey userKEK = keyManagementService.decryptUserKEK(user.getEncryptedKEK(), user.getKekEncryptionIV(), masterKEK);
        String privateKeyBase64 = Base64.getEncoder().encodeToString(privateKey);
        byte[] encryptedPrivateKey = keyManagementService.encryptSensitiveData(privateKeyBase64, userKEK, user.getKekEncryptionIV()); // Encrypt the private key

        // Store the encrypted private key in the User entity
        user.setEncryptedPrivateKey(encryptedPrivateKey);

        // Save the updated User entity to persist the encrypted private key
        userRepository.save(user);
    }
}
