package com.fyp14.OnePay.Transaction;

import com.fyp14.OnePay.Wallets.Wallet;
import com.fyp14.OnePay.Wallets.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletTransferRepository walletTransferRepository;

    @Transactional
    public String transfer(Long senderWalletId, Long recipientWalletId, BigDecimal amount) {
        Wallet senderWallet = walletRepository.findById(senderWalletId)
                .orElseThrow(() -> new RuntimeException("Sender wallet not found"));
        Wallet recipientWallet = walletRepository.findById(recipientWalletId)
                .orElseThrow(() -> new RuntimeException("Recipient wallet not found"));

        if (senderWallet.getBalance().compareTo(amount) < 0) {
            return "Insufficient balance";
        }

        // Debit the sender
        senderWallet.setBalance(senderWallet.getBalance().subtract(amount));
        walletRepository.save(senderWallet);

        // Credit the recipient
        recipientWallet.setBalance(recipientWallet.getBalance().add(amount));
        walletRepository.save(recipientWallet);

        // Record the sender's transaction as a debit
        Transaction senderTransaction = new Transaction();
        senderTransaction.setWallet(senderWallet);
        senderTransaction.setAmount(amount);
        senderTransaction.setTransactionType(TransactionType.DEBIT);
        senderTransaction.setTransactionDate(Instant.now());
        transactionRepository.save(senderTransaction);

        // Record the recipient's transaction as a credit
        Transaction recipientTransaction = new Transaction();
        recipientTransaction.setWallet(recipientWallet);
        recipientTransaction.setAmount(amount);
        recipientTransaction.setTransactionType(TransactionType.CREDIT);
        recipientTransaction.setTransactionDate(Instant.now());
        transactionRepository.save(recipientTransaction);

        // Log the transfer in the wallet_transfers table
        WalletTransfer walletTransfer = new WalletTransfer();
        walletTransfer.setSenderWalletId(senderWalletId);
        walletTransfer.setReceiverWalletId(recipientWalletId);
        walletTransfer.setAmount(amount);
        walletTransfer.setStatus("completed");
        walletTransfer.setTransactionId(UUID.randomUUID().toString()); // Generate a unique transaction ID
        walletTransfer.setTransferDate(Instant.now());
        walletTransferRepository.save(walletTransfer);

        return "Transfer successful";
    }
}
