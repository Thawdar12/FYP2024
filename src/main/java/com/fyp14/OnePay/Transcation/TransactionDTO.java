package com.fyp14.OnePay.Transcation;

import java.time.LocalDateTime;

public class TransactionDTO {
    private Long transactionID;
    private String amount;
    private TransactionType transactionType;
    private LocalDateTime timestamp;
    private TransactionStatus status;
    private String description;
    private Long fromWalletId;
    private Long toWalletId;

    public TransactionDTO(Transaction transaction, String decryptedAmount) {
        this.transactionID = transaction.getTransactionID();
        this.amount = decryptedAmount;
        this.transactionType = transaction.getTransactionType();
        this.timestamp = transaction.getTimestamp();
        this.status = transaction.getStatus();
        this.description = transaction.getDescription();

        // Set the new wallet fields
        if (transaction.getFromWallet() != null) {
            this.fromWalletId = transaction.getFromWallet().getWalletID();
        }
        if (transaction.getToWallet() != null) {
            this.toWalletId = transaction.getToWallet().getWalletID();
        }
    }

    public Long getTransactionID() {
        return transactionID;
    }

    public void setTransactionID(Long transactionID) {
        this.transactionID = transactionID;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getFromWalletId() {
        return fromWalletId;
    }

    public void setFromWalletId(Long fromWalletId) {
        this.fromWalletId = fromWalletId;
    }

    public Long getToWalletId() {
        return toWalletId;
    }

    public void setToWalletId(Long toWalletId) {
        this.toWalletId = toWalletId;
    }
}
