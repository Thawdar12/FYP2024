package com.fyp14.OnePay.FDS;

import com.fyp14.OnePay.KeyManagement.KEK.KeyManagementService;
import com.fyp14.OnePay.Transcation.*;
import com.fyp14.OnePay.User.User;
import com.fyp14.OnePay.Wallet.Wallet;
import com.fyp14.OnePay.Wallet.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class ConfirmationExpiryScheduler {

    private final TransactionConfirmationRepository transactionConfirmationRepository;
    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final KeyManagementService keyManagementService;

    @Autowired
    public ConfirmationExpiryScheduler(TransactionConfirmationRepository transactionConfirmationRepository,
                                       TransactionRepository transactionRepository,
                                       WalletRepository walletRepository,
                                       KeyManagementService keyManagementService) {
        this.transactionConfirmationRepository = transactionConfirmationRepository;
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.keyManagementService = keyManagementService;
    }

    /**
     * Scheduled task that runs every minute to check for expired confirmations.
     */
    @Scheduled(fixedRate = 60000) // Runs every 60 seconds
    @Transactional
    public void processExpiredConfirmations() {
        List<TransactionConfirmation> expiredConfirmations = transactionConfirmationRepository.findAll().stream()
                .filter(c -> c.getCreatedAt().plusMinutes(5).isBefore(LocalDateTime.now()))
                .toList();

        for (TransactionConfirmation confirmation : expiredConfirmations) {
            List<Transaction> transactions = confirmation.getTransactions();
            for (Transaction transaction : transactions) {
                // Mark as FAILED
                transaction.setStatus(TransactionStatus.FAILED);
                transactionRepository.save(transaction);

                // Revert the amount to sender's wallet
                if (transaction.getTransactionType() == TransactionType.TRANSFER) {
                    Wallet senderWallet = transaction.getFromWallet();
                    Wallet receiverWallet = transaction.getToWallet();
                    try {
                        BigDecimal amount = decryptTransactionAmount(transaction, senderWallet.getUser()); // Implement this method

                        // Revert the balance
                        senderWallet.setBalance(senderWallet.getBalance().add(amount));
                        walletRepository.save(senderWallet);
                        walletRepository.save(receiverWallet);
                    } catch (Exception e) {
                        // Handle decryption or other errors
                        e.printStackTrace();
                        // Optionally, log the error and continue
                    }
                }
            }

            // Remove the expired confirmation
            transactionConfirmationRepository.delete(confirmation);
        }
    }

    private BigDecimal decryptTransactionAmount(Transaction transaction, User user) throws Exception {
        SecretKey userKEK = keyManagementService.decryptUserKEK(user.getEncryptedKEK(), user.getKekEncryptionIV(), keyManagementService.getMasterKEKFromEnv()); // Ensure Fds has a public method or make it accessible
        String decryptedAmountStr = keyManagementService.decryptSensitiveData(transaction.getAmountEncrypted(), userKEK, transaction.getIv());
        return new BigDecimal(decryptedAmountStr);
    }
}
