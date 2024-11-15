package com.fyp14.OnePay.Transcation;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
public class TransactionConfirmation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String token;

    @ManyToMany
    private List<Transaction> transactions;

    private LocalDateTime createdAt;

    // Constructors
    public TransactionConfirmation() {
    }

    public TransactionConfirmation(String token, List<Transaction> transactions, LocalDateTime createdAt) {
        this.token = token;
        this.transactions = transactions;
        this.createdAt = createdAt;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
