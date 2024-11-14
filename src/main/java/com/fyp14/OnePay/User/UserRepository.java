package com.fyp14.OnePay.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByPhoneNumber(String phoneNumber);

    @Query("SELECT u FROM User u WHERE u.wallet.walletID = :walletID")
    User findByWalletId(@Param("walletID") Long walletID);

    User findByActivationToken(String activationToken);
}