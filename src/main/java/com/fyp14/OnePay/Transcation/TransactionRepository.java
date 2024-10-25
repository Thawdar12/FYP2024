package com.fyp14.OnePay.Transcation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    // Custom query to fetch transactions based on userID from either fromWallet or toWallet
    @Query("SELECT t FROM Transaction t WHERE t.fromWallet.user.userID = :userID OR t.toWallet.user.userID = :userID")
    List<Transaction> findByUserId(@Param("userID") Long userID);

}
