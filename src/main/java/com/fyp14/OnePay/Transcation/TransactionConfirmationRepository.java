package com.fyp14.OnePay.Transcation;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionConfirmationRepository extends JpaRepository<TransactionConfirmation, Long> {
    @EntityGraph(attributePaths = "transactions")
    List<TransactionConfirmation> findByCreatedAtBefore(LocalDateTime cutoffTime);

    Optional<TransactionConfirmation> findByToken(String token);
}
