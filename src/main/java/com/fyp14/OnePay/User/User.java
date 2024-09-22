package com.fyp14.OnePay.User;

import com.fyp14.OnePay.Wallet.Wallet;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "User")

public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userID;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(nullable = false)
    private Boolean locked = false;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true, length = 20)
    private String phoneNumber;

    @Column(nullable = false, columnDefinition = "DATETIME(3)")
    private LocalDateTime created_at;

    @Column(nullable = false, columnDefinition = "DATETIME(3)")
    private LocalDateTime updated_at;

    @OneToOne
    @JoinColumn(name = "wallet_id", referencedColumnName = "walletID", nullable = true)
    private Wallet wallet;

    @Lob
    @Column(name = "encrypted_kek", nullable = false)
    private byte[] encryptedKEK;

    @Lob
    @Column(name = "encrypted_private_key", columnDefinition = "MEDIUMBLOB")
    private byte[] encryptedPrivateKey;

    @Column(name = "kek_encryption_iv", nullable = false)
    private byte[] kekEncryptionIV;

    // Constructors

    public User() {
        this.created_at = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        this.updated_at = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
    }

    public User(String username, String email, String password, String phoneNumber) {
        this.username = username;
        this.email = email;
        this.enabled = true;
        this.locked = false;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.created_at = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        this.updated_at = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
    }

    // Getters and Setters

    public Long getUserID() {
        return userID;
    }

    public void setUserID(Long userID) {
        this.userID = userID;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getLocked() {
        return locked;
    }

    public void setLocked(Boolean locked) {
        this.locked = locked;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public LocalDateTime getCreated_at() {
        return created_at;
    }

    public void setCreated_at(LocalDateTime created_at) {
        this.created_at = created_at;
    }

    public LocalDateTime getUpdated_at() {
        return updated_at;
    }

    public void setUpdated_at(LocalDateTime updated_at) {
        this.updated_at = updated_at;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public void setWallet(Wallet wallet) {
        this.wallet = wallet;
    }

    public Long getWalletID() {
        if (this.wallet != null) {
            return this.wallet.getWalletID(); // Assuming Wallet entity has a getWalletID() method
        }
        return null; // Return null if wallet is not assigned
    }

    public byte[] getEncryptedKEK() {
        return encryptedKEK;
    }

    public void setEncryptedKEK(byte[] encryptedKEK) {
        this.encryptedKEK = encryptedKEK;
    }

    public byte[] getKekEncryptionIV() {
        return kekEncryptionIV;
    }

    public void setKekEncryptionIV(byte[] kekEncryptionIV) {
        this.kekEncryptionIV = kekEncryptionIV;
    }

    public byte[] getEncryptedPrivateKey() {
        return encryptedPrivateKey;
    }

    public void setEncryptedPrivateKey(byte[] encryptedPrivateKey) {
        this.encryptedPrivateKey = encryptedPrivateKey;
    }

    // JPA Callbacks for automatic timestamping

    @PrePersist
    protected void onCreate() {
        created_at = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        updated_at = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
    }

    @PreUpdate
    protected void onUpdate() {
        updated_at = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
    }
}
