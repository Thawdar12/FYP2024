package com.fyp14.OnePay.User;

import com.fyp14.OnePay.Wallet.Wallet;
import com.fyp14.OnePay.Wallet.WalletRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    public CustomUserDetailsService(UserRepository userRepository, WalletRepository walletRepository) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        // Fetch user from the database
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Fetch the wallet associated with the user by userID
        Long userID = user.getUserID();
        Wallet wallet = walletRepository.findByUserUserID(userID)
                .orElseThrow(() -> new RuntimeException("Wallet not found for user with ID: " + userID));


        // Return CustomUserDetails instance
        return new CustomUserDetails(user, wallet);
    }
}