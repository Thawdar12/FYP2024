package com.fyp14.OnePay.Wallets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class WalletService {

    @Autowired
    private WalletRepository walletRepository;

    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    // Method to create a wallet for a given user
    public Wallet createWallet(Wallet wallet) {
        // Save and return the wallet
        return walletRepository.save(wallet);
    }

    public Wallet getWalletByUserId(Long userId) {
        return walletRepository.findByUser_UserID(userId); // Updated method call
    }


    public void updateWalletBalance(Wallet wallet, BigDecimal amount) {
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);
    }

    public String topUpWallet(Long walletId, BigDecimal amount) {
        // Find the wallet by its ID
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        // Update the wallet balance
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        return "Top-up successful";
    }

    public Wallet getWalletBalance(Long walletId) {
        // Find the wallet and return it
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
    }

    // Method to create a new wallet
    public Wallet createWallet(String walletName, BigDecimal initialBalance) {
        Wallet wallet = new Wallet();
        wallet.setName(walletName);
        wallet.setBalance(initialBalance);

        // Save and return the created wallet
        return walletRepository.save(wallet);
    }

    public void transferFunds(Long senderWalletId, Long recipientWalletId, Double amount) {

    }
}