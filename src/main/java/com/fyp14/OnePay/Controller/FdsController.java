package com.fyp14.OnePay.Controller;

import com.fyp14.OnePay.FDS.Fds;
import com.fyp14.OnePay.KeyManagement.KEK.KeyManagementService;
import com.fyp14.OnePay.Transcation.*;
import com.fyp14.OnePay.User.User;
import com.fyp14.OnePay.Wallet.Wallet;
import com.fyp14.OnePay.Wallet.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/fds")
public class FdsController {

    private final Fds fds;
    private final TransactionConfirmationRepository transactionConfirmationRepository;
    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final KeyManagementService keyManagementService;

    @Autowired
    public FdsController(Fds fds, TransactionConfirmationRepository transactionConfirmationRepository,
                         TransactionRepository transactionRepository, WalletRepository walletRepository, KeyManagementService keyManagementService) {
        this.fds = fds;
        this.transactionConfirmationRepository = transactionConfirmationRepository;
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.keyManagementService = keyManagementService;
    }

    /**
     * Endpoint to confirm a suspicious transaction via email link.
     *
     * @param token The unique confirmation token.
     * @return A response message indicating the outcome.
     */
    @GetMapping("/confirmTransaction")
    public String confirmTransaction(@RequestParam("token") String token) {
        try {
            TransactionConfirmation confirmation = transactionConfirmationRepository.findByToken(token)
                    .orElseThrow(() -> new Exception("Invalid or expired confirmation token."));

            // Check if token is within 5 minutes
            if (confirmation.getCreatedAt().plusMinutes(5).isBefore(LocalDateTime.now())) {
                throw new Exception("Confirmation link has expired.");
            }

            // Update transactions to COMPLETED
            List<Transaction> transactions = confirmation.getTransactions();
            for (Transaction transaction : transactions) {
                transaction.setStatus(TransactionStatus.COMPLETED);
                transactionRepository.save(transaction);

                // Update wallet balances if necessary
                if (transaction.getTransactionType() == TransactionType.TRANSFER) {
                    Wallet senderWallet = transaction.getFromWallet();
                    Wallet receiverWallet = transaction.getToWallet();
                    BigDecimal amount = decryptTransactionAmount(transaction, senderWallet.getUser()); // Implement this method
                    senderWallet.setBalance(senderWallet.getBalance().subtract(amount));
                    receiverWallet.setBalance(receiverWallet.getBalance().add(amount));
                    walletRepository.save(senderWallet);
                    walletRepository.save(receiverWallet);
                }
            }

            // Remove the confirmation record
            transactionConfirmationRepository.delete(confirmation);

            return "Transaction confirmed successfully.";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    // Implement decryptTransactionAmount as needed
    private BigDecimal decryptTransactionAmount(Transaction transaction, User user) throws Exception {
        SecretKey userKEK = keyManagementService.decryptUserKEK(user.getEncryptedKEK(), user.getKekEncryptionIV(), keyManagementService.getMasterKEKFromEnv()); // Ensure Fds has a public method or make it accessible
        String decryptedAmountStr = keyManagementService.decryptSensitiveData(transaction.getAmountEncrypted(), userKEK, transaction.getIv());
        return new BigDecimal(decryptedAmountStr);
    }
}
