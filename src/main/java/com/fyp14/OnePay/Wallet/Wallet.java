package com.fyp14.OnePay.Wallet;

import com.fyp14.OnePay.Transcation.Transaction;
import com.fyp14.OnePay.User.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Entity
@Table(name = "Wallet")
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long walletID;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false, columnDefinition = "DATETIME(3)")
    private LocalDateTime created_at;

    @Column(nullable = false, columnDefinition = "DATETIME(3)")
    private LocalDateTime updated_at;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;  // Bidirectional relationship to User

    @OneToMany(mappedBy = "fromWallet", cascade = CascadeType.ALL)
    private List<Transaction> sentTransactions;

    @OneToMany(mappedBy = "toWallet", cascade = CascadeType.ALL)
    private List<Transaction> receivedTransactions;

    // Constructors

    public Wallet() {
        this.balance = BigDecimal.ZERO;
        this.created_at = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        this.updated_at = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
    }

    public Wallet(String privateKey, User user) {
        this.user = user;
        this.balance = BigDecimal.ZERO;
        this.created_at = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        this.updated_at = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
    }

    // Getters and Setters

    public Long getWalletID() {
        return walletID;
    }

    public void setWalletID(Long walletID) {
        this.walletID = walletID;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<Transaction> getSentTransactions() {
        return sentTransactions;
    }

    public void setSentTransactions(List<Transaction> sentTransactions) {
        this.sentTransactions = sentTransactions;
    }

    public List<Transaction> getReceivedTransactions() {
        return receivedTransactions;
    }

    public void setReceivedTransactions(List<Transaction> receivedTransactions) {
        this.receivedTransactions = receivedTransactions;
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
