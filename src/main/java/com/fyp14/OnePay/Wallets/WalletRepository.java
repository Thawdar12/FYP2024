package com.fyp14.OnePay.Wallets;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Wallet findByUser_UserID(Long userId); // Change to 'UserID'
}

