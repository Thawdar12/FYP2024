package com.fyp14.OnePay.Wallet;

import com.fyp14.OnePay.FDS.Fds;
import com.fyp14.OnePay.KeyManagement.KEK.KeyManagementService;
import com.fyp14.OnePay.Security.HashingService;
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
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.List;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final KeyManagementService keyManagementService;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final HashingService hashingService;
    private final Fds fds;

    public WalletService(WalletRepository walletRepository, KeyManagementService keyManagementService,
                         TransactionRepository transactionRepository, UserRepository userRepository,
                         HashingService hashingService, Fds fds) {
        this.walletRepository = walletRepository;
        this.keyManagementService = keyManagementService;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.hashingService = hashingService;
        this.fds = fds;
    }

    // Helper methods

    private SecretKey decryptUserKEK(User user) throws Exception {
        return keyManagementService.decryptUserKEK(user.getEncryptedKEK(), user.getKekEncryptionIV(), keyManagementService.getMasterKEKFromEnv());
    }

    private byte[] encryptTransactionAmount(BigDecimal amount, SecretKey userKEK, byte[] iv) throws Exception {
        return keyManagementService.encryptSensitiveData(amount.toString(), userKEK, iv);
    }

    private PrivateKey decryptUserPrivateKey(User user, SecretKey userKEK) throws Exception {
        byte[] encryptedPrivateKey = user.getEncryptedPrivateKey();
        String decryptedPrivateKeyBase64 = keyManagementService.decryptSensitiveData(encryptedPrivateKey, userKEK, user.getKekEncryptionIV());
        byte[] decryptedPrivateKeyBytes = Base64.getDecoder().decode(decryptedPrivateKeyBase64);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(decryptedPrivateKeyBytes);
        return keyFactory.generatePrivate(privateKeySpec);
    }

    private byte[] signTransactionHash(String hash, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(hash.getBytes(StandardCharsets.UTF_8));
        return signature.sign();
    }

    private Transaction createTransaction(byte[] encryptedAmount, byte[] iv, TransactionType type, Wallet fromWallet, Wallet toWallet, String description, String previousTransactionHash, TransactionStatus status) throws Exception {
        Transaction transaction = new Transaction();
        transaction.setAmountEncrypted(encryptedAmount);
        transaction.setIv(iv);
        transaction.setTransactionType(type);
        transaction.setFromWallet(fromWallet);
        transaction.setToWallet(toWallet);
        transaction.setStatus(status);
        transaction.setDescription(description);
        transaction.setPreviousTransactionHash(previousTransactionHash);
        // currentTransactionHash and digitalSignature will be set after computing them
        return transaction;
    }

    private String getLastTransactionHash(Wallet wallet) {
        List<Transaction> transactions = transactionRepository.findTopTransactionsByWalletOrderByTimestampDesc(wallet.getWalletID());
        if (!transactions.isEmpty()) {
            return transactions.get(0).getCurrentTransactionHash();
        } else {
            // No previous transaction, so hash the master key and return that
            SecretKey masterKey = keyManagementService.getMasterKEKFromEnv(); // Retrieve the master key
            byte[] masterKeyBytes = masterKey.getEncoded();
            String masterKeyBase64 = Base64.getEncoder().encodeToString(masterKeyBytes);
            String masterKeyHash = hashingService.hashSHA256(masterKeyBase64);
            return masterKeyHash;
        }
    }

    private String computeTransactionHash(Transaction transaction) throws Exception {
        String dataToHash = transaction.getPreviousTransactionHash()
                + Base64.getEncoder().encodeToString(transaction.getAmountEncrypted())
                + Base64.getEncoder().encodeToString(transaction.getIv())
                + transaction.getTransactionType().toString()
                + (transaction.getFromWallet() != null ? transaction.getFromWallet().getWalletID().toString() : "0")
                + (transaction.getToWallet() != null ? transaction.getToWallet().getWalletID().toString() : "0")
                + transaction.getTimestamp().toString()
                + (transaction.getDescription() != null ? transaction.getDescription() : "");
        return hashingService.hashSHA256(dataToHash);
    }

    @Transactional
    public void topUpWallet(User user, Wallet wallet, BigDecimal amount, HttpSession session) throws Exception {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        if (wallet == null) {
            throw new Exception("Wallet not found for user");
        }

        SecretKey userKEK = decryptUserKEK(user);
        byte[] iv = keyManagementService.generateRandomIV();
        byte[] encryptedAmount = encryptTransactionAmount(amount, userKEK, iv);

        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        String previousTransactionHash = getLastTransactionHash(wallet);

        Transaction transaction = createTransaction(
                encryptedAmount,
                iv,
                TransactionType.DEPOSIT,
                null,
                wallet,
                "Top-up to wallet",
                previousTransactionHash,
                TransactionStatus.valueOf("COMPLETED")
        );

        transactionRepository.save(transaction); // Save to generate timestamp

        String currentTransactionHash = computeTransactionHash(transaction);
        transaction.setCurrentTransactionHash(currentTransactionHash);

        PrivateKey privateKey = decryptUserPrivateKey(user, userKEK);
        byte[] digitalSignature = signTransactionHash(currentTransactionHash, privateKey);
        transaction.setDigitalSignature(digitalSignature);

        transactionRepository.save(transaction);

        session.setAttribute("balance", wallet.getBalance());
    }

    @Transactional
    public void withdrawFromWallet(User user, Wallet wallet, BigDecimal amount, HttpSession session) throws Exception {
        // 1. Validate that the withdrawal amount is more than 0.
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        // 2. Check if the wallet exists for the user.
        if (wallet == null) {
            throw new Exception("Wallet not found for user");
        }

        // 3. Validate sufficient balance in the wallet.
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new Exception("Insufficient balance in wallet");
        }

        // 4. Decrypt the user's Key Encryption Key (KEK).
        SecretKey userKEK = decryptUserKEK(user);

        // 5. Generate a random Initialization Vector (IV) for encryption.
        byte[] iv = keyManagementService.generateRandomIV();

        // 6. Encrypt the transaction amount using the user's KEK and the generated IV.
        byte[] encryptedAmount = encryptTransactionAmount(amount, userKEK, iv);

        // 7. Deduct the amount from the wallet's balance and save the updated wallet.
        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);

        // 8. Retrieve the hash of the previous transaction for audit purposes.
        String previousTransactionHash = getLastTransactionHash(wallet);

        // 9. Create a new transaction record with the encrypted amount and other details.
        Transaction transaction = createTransaction(
                encryptedAmount,
                iv,
                TransactionType.WITHDRAWAL,
                wallet,
                null,
                "Withdrawal from wallet",
                previousTransactionHash,
                TransactionStatus.valueOf("COMPLETED")
        );

        // 10. Save the transaction to the repository to generate a timestamp.
        transactionRepository.save(transaction);

        // 11. Compute the hash of the current transaction for integrity verification.
        String currentTransactionHash = computeTransactionHash(transaction);
        transaction.setCurrentTransactionHash(currentTransactionHash);

        // 12. Decrypt the user's private key using their KEK.
        PrivateKey privateKey = decryptUserPrivateKey(user, userKEK);

        // 13. Sign the transaction hash using the decrypted private key to ensure authenticity.
        byte[] digitalSignature = signTransactionHash(currentTransactionHash, privateKey);
        transaction.setDigitalSignature(digitalSignature);

        // 14. Save the signed transaction back to the repository.
        transactionRepository.save(transaction);

        // 15. Update the session with the new wallet balance.
        session.setAttribute("balance", wallet.getBalance());
    }

    @Transactional
    public void transferMoney(HttpSession session, String receiverPhoneNumber, BigDecimal amount) throws Exception {
        Long userID = (Long) session.getAttribute("userID");
        if (userID == null) {
            throw new Exception("User is not authenticated");
        }

        User sender = userRepository.findById(userID).orElseThrow(() -> new Exception("Sender not found"));
        Wallet senderWallet = sender.getWallet();

        if (senderWallet.getBalance().compareTo(amount) < 0) {
            throw new Exception("Insufficient balance");
        }

        User receiver = userRepository.findByPhoneNumber(receiverPhoneNumber).orElseThrow(() -> new Exception("Receiver not found"));
        Wallet receiverWallet = receiver.getWallet();

        SecretKey senderKEK = decryptUserKEK(sender);
        SecretKey receiverKEK = decryptUserKEK(receiver);

        byte[] ivSender = keyManagementService.generateRandomIV();
        byte[] encryptedAmountSender = encryptTransactionAmount(amount, senderKEK, ivSender);

        byte[] ivReceiver = keyManagementService.generateRandomIV();
        byte[] encryptedAmountReceiver = encryptTransactionAmount(amount, receiverKEK, ivReceiver);

        String senderPreviousHash = getLastTransactionHash(senderWallet);

        // Sender Transaction (TRANSFER)
        Transaction senderTransaction = createTransaction(
                encryptedAmountSender,
                ivSender,
                TransactionType.TRANSFER,
                senderWallet,
                receiverWallet,
                "Transfer to " + receiver.getUsername(),
                senderPreviousHash,
                TransactionStatus.valueOf("PENDING")
        );
        transactionRepository.save(senderTransaction); // Save to generate timestamp

        String senderCurrentHash = computeTransactionHash(senderTransaction);
        senderTransaction.setCurrentTransactionHash(senderCurrentHash);

        PrivateKey senderPrivateKey = decryptUserPrivateKey(sender, senderKEK);
        byte[] senderSignature = signTransactionHash(senderCurrentHash, senderPrivateKey);
        senderTransaction.setDigitalSignature(senderSignature);

        transactionRepository.save(senderTransaction);

        // Receiver Transaction (DEPOSIT)
        // Use the sender's current hash as the receiver's previous hash
        Transaction receiverTransaction = createTransaction(
                encryptedAmountReceiver,
                ivReceiver,
                TransactionType.DEPOSIT,
                senderWallet,
                receiverWallet,
                "Received from " + sender.getUsername(),
                senderCurrentHash,
                TransactionStatus.valueOf("PENDING")
        );
        transactionRepository.save(receiverTransaction); // Save to generate timestamp

        String receiverCurrentHash = computeTransactionHash(receiverTransaction);
        receiverTransaction.setCurrentTransactionHash(receiverCurrentHash);

        PrivateKey receiverPrivateKey = decryptUserPrivateKey(receiver, receiverKEK);
        byte[] receiverSignature = signTransactionHash(receiverCurrentHash, receiverPrivateKey);
        receiverTransaction.setDigitalSignature(receiverSignature);

        transactionRepository.save(receiverTransaction);

        fds.fdsComputation(senderTransaction, receiverTransaction, amount, sender, receiver);
    }
}
