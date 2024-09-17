package com.fyp14.OnePay.Transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WalletTransferRepository extends JpaRepository<WalletTransfer, Long> {
    // Custom query methods can be added here
}
