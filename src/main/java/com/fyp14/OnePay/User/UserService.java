package com.fyp14.OnePay.User;

import com.fyp14.OnePay.Wallets.Wallet;
import com.fyp14.OnePay.Wallets.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final WalletService walletService;
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);


    @Autowired
    public UserService(UserRepository userRepository, WalletService walletService) {
        this.userRepository = userRepository;
        this.walletService = walletService;
    }

    // User registration method
    @Transactional
    public User registerUser(User user) {
        // Save the user to the database
        User savedUser = userRepository.save(user);

        // Create a wallet for the new user after the user has been saved
        createWalletForUser(savedUser);

        return savedUser;
    }

    // Helper method to create a wallet for a user
    private void createWalletForUser(User user) {
        // Create a new wallet instance
        Wallet wallet = new Wallet();
        wallet.setUser(user);  // Associate the user with the wallet
        wallet.setName(user.getUsername() + "'s Wallet");  // Set wallet name
        wallet.setBalance(BigDecimal.ZERO);  // Initial balance (can be set to a different value if needed)

        // Save the wallet using WalletService
        walletService.createWallet(wallet);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> user = userRepository.findByUsername(username);

        if (user.isPresent()) {
            User userObj = user.get();
            return org.springframework.security.core.userdetails.User.builder()
                    .username(userObj.getUsername())
                    .password(userObj.getPassword())
                    .roles(userObj.getUserRole().toString())
                    .build();
        } else {
            throw new UsernameNotFoundException(username);
        }
    }
}



