package com.fyp14.OnePay.Controller;

public class TransferRequest {
    private Long senderWalletId;
    private Long recipientWalletId;
    private Double amount;

    // Getters and Setters
    public Long getSenderWalletId() {
        return senderWalletId;
    }

    public void setSenderWalletId(Long senderWalletId) {
        this.senderWalletId = senderWalletId;
    }

    public Long getRecipientWalletId() {
        return recipientWalletId;
    }

    public void setRecipientWalletId(Long recipientWalletId) {
        this.recipientWalletId = recipientWalletId;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }
}
