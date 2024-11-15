//this service handle actual user registration logic
//and save entry into database

package com.fyp14.OnePay.User;

import com.fyp14.OnePay.KeyManagement.KEK.KeyManagementService;
import com.fyp14.OnePay.KeyManagement.RSA.UserRSAKeyService;
import com.fyp14.OnePay.Mail.MailService;
import com.fyp14.OnePay.Wallet.Wallet;
import com.fyp14.OnePay.Wallet.WalletRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final KeyManagementService keyManagementService;
    private final UserRSAKeyService userRSAKeyService;
    private final MailService mailService;

    public UserService(UserRepository userRepository,
                       WalletRepository walletRepository,
                       PasswordEncoder passwordEncoder,
                       KeyManagementService keyManagementService,
                       UserRSAKeyService userRSAKeyService,
                       MailService mailService) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.passwordEncoder = passwordEncoder;
        this.keyManagementService = keyManagementService;
        this.userRSAKeyService = userRSAKeyService;
        this.mailService = mailService;
    }

    @Transactional
    public void registerUser(User user) throws Exception {
        // Step 1: Encode the user's password
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Step 2: Process and encrypt the user's KEK
        keyManagementService.processUserKEK(user);  // Generate, encrypt and set encrypted KEK & IV

        // Step 3: Set user as disabled and generate activation token
        user.setEnabled(false);
        String token = UUID.randomUUID().toString();
        user.setActivationToken(token);

        // Step 3: Save the user with encrypted KEK (wallet not created)
        User savedUser = userRepository.save(user);
        mailService.sendActivationEmail(savedUser);

        // Step 4: Create and save the wallet
        Wallet wallet = new Wallet();
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setUser(savedUser);  // Associate the wallet with the user
        wallet.setCreated_at(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
        walletRepository.save(wallet);

        // Step 5: Update the user with the wallet
        savedUser.setWallet(wallet);  // Associate wallet with the user
        userRepository.save(savedUser);  // Update user with walletID

        // Step 6: Create RSA key for user
        userRSAKeyService.generateAndStoreKeyPairs(user);
    }
}
