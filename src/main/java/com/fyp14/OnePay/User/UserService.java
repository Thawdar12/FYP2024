//this service handle actual user registration logic
//and save entry into database

package com.fyp14.OnePay.User;

import com.fyp14.OnePay.Security.KeyManagementService;
import com.fyp14.OnePay.Wallet.Wallet;
import com.fyp14.OnePay.Wallet.WalletRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final KeyManagementService keyManagementService; // Inject KeyManagementService

    public UserService(UserRepository userRepository,
                       WalletRepository walletRepository,
                       PasswordEncoder passwordEncoder,
                       KeyManagementService keyManagementService) {  // Add KeyManagementService to constructor
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.passwordEncoder = passwordEncoder;
        this.keyManagementService = keyManagementService;
    }

    @Transactional
    public void registerUser(User user) throws Exception {
        // Step 1: Encode the user's password
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Step 2: Process and encrypt the user's KEK
        keyManagementService.processUserKEK(user);  // Generate, encrypt and set encrypted KEK & IV

        // Step 3: Save the user with encrypted KEK
        User savedUser = userRepository.save(user);

        // Step 4: Create and save the user's wallet
        Wallet wallet = new Wallet();
        wallet.setUser(savedUser);
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setCreated_at(LocalDateTime.now());
        walletRepository.save(wallet);
    }
}
