//this code handle actual translation logic
//top and transfer logic is handled here

package com.fyp14.OnePay.Wallet;

import com.fyp14.OnePay.Security.KeyManagementService;
import com.fyp14.OnePay.Transcation.Transaction;
import com.fyp14.OnePay.Transcation.TransactionRepository;
import com.fyp14.OnePay.Transcation.TransactionStatus;
import com.fyp14.OnePay.Transcation.TransactionType;
import com.fyp14.OnePay.User.User;
import com.fyp14.OnePay.User.UserRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.util.Base64;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final KeyManagementService keyManagementService;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;


    public WalletService(WalletRepository walletRepository, KeyManagementService keyManagementService, TransactionRepository transactionRepository, UserRepository userRepository) {
        this.walletRepository = walletRepository;
        this.keyManagementService = keyManagementService;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    //method that handles topUp
    @Transactional
    public void topUpWallet(User user, Wallet wallet, BigDecimal amount, HttpSession session) throws Exception {
        //first, check if amount is valid
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        if (wallet == null) {
            throw new Exception("Wallet not found for user");
        }

        //1: Decrypt the user's KEK using the Master KEK
        SecretKey userKEK = keyManagementService.decryptUserKEK(user.getEncryptedKEK(), user.getKekEncryptionIV(), keyManagementService.getMasterKEKFromEnv());

        //2: Encrypt the transaction amount using the user's KEK
        byte[] iv = keyManagementService.generateRandomIV(); // Generate a new IV for the transaction
        byte[] encryptedAmountBytes = keyManagementService.encryptSensitiveData(amount.toString(), userKEK, iv); // Encrypt the amount
        String encryptedAmount = Base64.getEncoder().encodeToString(encryptedAmountBytes); // Encode to Base64

        //3: Update wallet balance
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        //4: Create a transaction record with the encrypted amount
        Transaction transaction = new Transaction();
        transaction.setAmountEncrypted(encryptedAmount); // Save the Base64-encoded encrypted amount
        transaction.setIv(iv); // Save the IV for decryption
        transaction.setTransactionType(TransactionType.DEPOSIT);
        transaction.setFromWallet(null); // Null since this is a top-up
        transaction.setToWallet(wallet);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setDescription("Top-up to wallet");

        transactionRepository.save(transaction);

        // After top-up, update the session with the updated balance
        BigDecimal updatedBalance = wallet.getBalance();
        session.setAttribute("balance", updatedBalance); // Update balance in session
    }

    //method that handles transfer
    @Transactional
    public void transferMoney(HttpSession session, String receiverPhoneNumber, BigDecimal amount) throws Exception {
        //1: Retrieve the userID from session (sender)
        Long userID = (Long) session.getAttribute("userID");
        if (userID == null) {
            throw new Exception("User is not authenticated");
        }

        User sender = userRepository.findById(userID)
                .orElseThrow(() -> new Exception("Sender not found"));
        Wallet senderWallet = sender.getWallet();

        //2: Check if the user has enough balance
        if (senderWallet.getBalance().compareTo(amount) < 0) {
            throw new Exception("Insufficient balance");
        }

        //3: Find the receiver by phone number
        User receiver = userRepository.findByPhoneNumber(receiverPhoneNumber)
                .orElseThrow(() -> new Exception("Receiver not found"));
        Wallet receiverWallet = receiver.getWallet();

        //4: Decrypt the sender's KEK using the Master KEK
        SecretKey senderKEK = keyManagementService.decryptUserKEK(sender.getEncryptedKEK(), sender.getKekEncryptionIV(), keyManagementService.getMasterKEKFromEnv());

        //5: Decrypt the receiver's KEK using the Master KEK
        SecretKey receiverKEK = keyManagementService.decryptUserKEK(receiver.getEncryptedKEK(), receiver.getKekEncryptionIV(), keyManagementService.getMasterKEKFromEnv());
        //side note: both decryption is done on server,
        //as for why we need decrypt both key because we need them to update entry for both party later

        //6: Encrypt the transaction amount using both sender and receiver's KEK
        byte[] ivSender = keyManagementService.generateRandomIV(); // Generate IV for sender transaction
        byte[] encryptedAmountSenderBytes = keyManagementService.encryptSensitiveData(amount.toString(), senderKEK, ivSender);
        String encryptedAmountSender = Base64.getEncoder().encodeToString(encryptedAmountSenderBytes); // Encrypt amount for sender

        byte[] ivReceiver = keyManagementService.generateRandomIV(); // Generate IV for receiver transaction
        byte[] encryptedAmountReceiverBytes = keyManagementService.encryptSensitiveData(amount.toString(), receiverKEK, ivReceiver);
        String encryptedAmountReceiver = Base64.getEncoder().encodeToString(encryptedAmountReceiverBytes); // Encrypt amount for receiver

        //7: Deduct amount from sender's wallet
        senderWallet.setBalance(senderWallet.getBalance().subtract(amount));
        walletRepository.save(senderWallet);

        //8: Credit amount to receiver's wallet
        receiverWallet.setBalance(receiverWallet.getBalance().add(amount));
        walletRepository.save(receiverWallet);

        //9: Record the transaction for the sender (debit)
        Transaction senderTransaction = new Transaction();
        senderTransaction.setAmountEncrypted(encryptedAmountSender);
        senderTransaction.setIv(ivSender);
        senderTransaction.setTransactionType(TransactionType.TRANSFER);
        senderTransaction.setFromWallet(senderWallet);
        senderTransaction.setToWallet(receiverWallet);
        senderTransaction.setStatus(TransactionStatus.COMPLETED);
        senderTransaction.setDescription("Transfer to " + receiver.getUsername());
        transactionRepository.save(senderTransaction);

        //10: Record the transaction for the receiver (credit)
        Transaction receiverTransaction = new Transaction();
        receiverTransaction.setAmountEncrypted(encryptedAmountReceiver);
        receiverTransaction.setIv(ivReceiver);
        receiverTransaction.setTransactionType(TransactionType.DEPOSIT);
        receiverTransaction.setFromWallet(senderWallet);
        receiverTransaction.setToWallet(receiverWallet);
        receiverTransaction.setStatus(TransactionStatus.COMPLETED);
        receiverTransaction.setDescription("Received from " + sender.getUsername());
        transactionRepository.save(receiverTransaction);

        // After transfer, update the session with the updated balance
        BigDecimal updatedBalance = senderWallet.getBalance();
        session.setAttribute("balance", updatedBalance); // Update balance in session
    }
}
