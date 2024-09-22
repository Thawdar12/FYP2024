package com.fyp14.OnePay.KeyManagement.RSA;

import com.fyp14.OnePay.User.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
public class UserRSAKey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Primary key for the entity

    @OneToOne
    @JoinColumn(name = "user_id", nullable = true, unique = true) // foreign key column in this table
    private User user;

    @Column(nullable = false, length = 2048) // RSA public keys can be long, set a high length
    private String userPublicKey; // Public key stored as a Base64 encoded string

    @Column(nullable = false, columnDefinition = "DATETIME(3)")
    private LocalDateTime createdAt; // Timestamp when the key was generated

    //constructor
    public UserRSAKey() {
    }

    public UserRSAKey(Long userId, String userPublicKey) {
        this.user = user;
        this.userPublicKey = userPublicKey;
        this.createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
    }

    //Getter and Setter

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getUserPublicKey() {
        return userPublicKey;
    }

    public void setUserPublicKey(String userPublicKey) {
        this.userPublicKey = userPublicKey;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
