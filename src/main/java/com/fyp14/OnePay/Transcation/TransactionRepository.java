package com.fyp14.OnePay.Transcation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    // Custom query to fetch transactions based on userID from either fromWallet or toWallet
    @Query("SELECT t FROM Transaction t LEFT JOIN t.fromWallet fw LEFT JOIN fw.user fwu LEFT JOIN t.toWallet tw LEFT JOIN tw.user twu WHERE (fwu.userID = :userID) OR (twu.userID = :userID)")
    List<Transaction> findByUserId(@Param("userID") Long userID);

    @Query("SELECT t FROM Transaction t WHERE (t.fromWallet.walletID = :walletID OR t.toWallet.walletID = :walletID) ORDER BY t.timestamp DESC")
    List<Transaction> findTopTransactionsByWalletOrderByTimestampDesc(@Param("walletID") Long walletID);

    List<Transaction> findAllByOrderByTransactionIDAsc();

    @Query("SELECT w.walletID FROM Wallet w WHERE w.user.userID = :userID")
    List<Long> findWalletIdsByUserId(@Param("userID") Long userID);

}
