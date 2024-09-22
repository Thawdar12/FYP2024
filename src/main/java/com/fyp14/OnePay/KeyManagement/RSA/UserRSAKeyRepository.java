package com.fyp14.OnePay.KeyManagement.RSA;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRSAKeyRepository extends JpaRepository<UserRSAKey, Long> {
    // Find UserRSAKey by userId
    UserRSAKey findByUser_UserID(Long userId);
}
