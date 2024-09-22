package com.fyp14.OnePay.Transcation;

import com.fyp14.OnePay.Wallet.Wallet;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "Transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionID;

    @Lob
    @Column(nullable = false)
    private byte[] amountEncrypted;

    @Column(nullable = false)
    private byte[] iv;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType transactionType;

    @Column(nullable = false, columnDefinition = "DATETIME(3)")
    private LocalDateTime timestamp;

    @ManyToOne
    @JoinColumn(name = "from_wallet_id")
    private Wallet fromWallet;

    @ManyToOne
    @JoinColumn(name = "to_wallet_id")
    private Wallet toWallet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Column(nullable = true)
    private String description;

    @Column(nullable = true, length = 64)
    private String hashValueOfTransaction;

    @Lob
    @Column(nullable = true, columnDefinition = "MEDIUMBLOB")
    private byte[] digitalSignature;

    // Constructors

    public Transaction() {
        this.timestamp = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        this.status = TransactionStatus.PENDING;
    }

    public Transaction(byte[] amountEncrypted, byte[] iv, TransactionType transactionType, Wallet fromWallet, Wallet toWallet, String description) {
        this.amountEncrypted = amountEncrypted;
        this.iv = iv;
        this.transactionType = transactionType;
        this.fromWallet = fromWallet;
        this.toWallet = toWallet;
        this.description = description;
        this.timestamp = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        this.status = TransactionStatus.PENDING;
    }

    // Getters and Setters

    public Long getTransactionID() {
        return transactionID;
    }

    public void setTransactionID(Long transactionID) {
        this.transactionID = transactionID;
    }

    public byte[] getAmountEncrypted() {
        return amountEncrypted;
    }

    public void setAmountEncrypted(byte[] amountEncrypted) {
        this.amountEncrypted = amountEncrypted;
    }

    public byte[] getIv() {
        return iv;
    }

    public void setIv(byte[] iv) {
        this.iv = iv;
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

    // No setter for timestamp as it's set at creation

    public Wallet getFromWallet() {
        return fromWallet;
    }

    public void setFromWallet(Wallet fromWallet) {
        this.fromWallet = fromWallet;
    }

    public Wallet getToWallet() {
        return toWallet;
    }

    public void setToWallet(Wallet toWallet) {
        this.toWallet = toWallet;
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

    public String getHashValueOfTransaction() {
        return hashValueOfTransaction;
    }

    public void setHashValueOfTransaction(String hashValueOfTransaction) {
        this.hashValueOfTransaction = hashValueOfTransaction;
    }

    public byte[] getDigitalSignature() {
        return digitalSignature;
    }

    public void setDigitalSignature(byte[] digitalSignature) {
        this.digitalSignature = digitalSignature;
    }

    public Long getFromWalletID() {
        return (fromWallet != null) ? fromWallet.getWalletID() : null;
    }

    public Long getToWalletID() {
        return (toWallet != null) ? toWallet.getWalletID() : null;
    }
}
