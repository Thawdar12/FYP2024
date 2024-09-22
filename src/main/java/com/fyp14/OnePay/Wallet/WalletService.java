package com.fyp14.OnePay.Wallet;

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

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final KeyManagementService keyManagementService;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final HashingService hashingService;

    public WalletService(WalletRepository walletRepository, KeyManagementService keyManagementService,
                         TransactionRepository transactionRepository, UserRepository userRepository,
                         HashingService hashingService) {
        this.walletRepository = walletRepository;
        this.keyManagementService = keyManagementService;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.hashingService = hashingService;
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

    private Transaction createTransaction(byte[] encryptedAmount, byte[] iv, TransactionType type, Wallet fromWallet, Wallet toWallet, String description) {
        Transaction transaction = new Transaction();
        transaction.setAmountEncrypted(encryptedAmount); // Updated to handle byte[]
        transaction.setIv(iv);
        transaction.setTransactionType(type);
        transaction.setFromWallet(fromWallet);
        transaction.setToWallet(toWallet);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setDescription(description);
        return transaction;
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
        byte[] encryptedAmount = encryptTransactionAmount(amount, userKEK, iv); // Now handling byte[] directly

        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        // Create transaction record first without hash or signature
        Transaction transaction = createTransaction(encryptedAmount, iv, TransactionType.DEPOSIT, null, wallet, "Top-up to wallet");
        transactionRepository.save(transaction);

        // Now create the hash after the transaction is saved (to ensure timestamp is accurate)
        String hash = hashingService.generateTransactionHash("null", wallet.getWalletID().toString(), amount.toString(), transaction.getTimestamp());
        transaction.setHashValueOfTransaction(hash);

        // Sign the transaction hash
        PrivateKey privateKey = decryptUserPrivateKey(user, userKEK);
        byte[] digitalSignature = signTransactionHash(hash, privateKey);
        transaction.setDigitalSignature(digitalSignature); // Updated to handle byte[] for digitalSignature

        // Update the transaction with the hash and signature
        transactionRepository.save(transaction);

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
        byte[] encryptedAmountSender = encryptTransactionAmount(amount, senderKEK, ivSender); // Now byte[]

        byte[] ivReceiver = keyManagementService.generateRandomIV();
        byte[] encryptedAmountReceiver = encryptTransactionAmount(amount, receiverKEK, ivReceiver); // Now byte[]

        senderWallet.setBalance(senderWallet.getBalance().subtract(amount));
        walletRepository.save(senderWallet);

        receiverWallet.setBalance(receiverWallet.getBalance().add(amount));
        walletRepository.save(receiverWallet);

        // Create sender transaction first without hash or signature
        Transaction senderTransaction = createTransaction(encryptedAmountSender, ivSender, TransactionType.TRANSFER, senderWallet, receiverWallet, "Transfer to " + receiver.getUsername());
        transactionRepository.save(senderTransaction);

        // Now create the hash and sign for sender transaction
        String senderHash = hashingService.generateTransactionHash(senderWallet.getWalletID().toString(), receiverWallet.getWalletID().toString(), amount.toString(), senderTransaction.getTimestamp());
        senderTransaction.setHashValueOfTransaction(senderHash);
        PrivateKey senderPrivateKey = decryptUserPrivateKey(sender, senderKEK);
        byte[] senderSignature = signTransactionHash(senderHash, senderPrivateKey);
        senderTransaction.setDigitalSignature(senderSignature); // Updated to handle byte[] for digitalSignature
        transactionRepository.save(senderTransaction);

        // Create receiver transaction first without hash or signature
        Transaction receiverTransaction = createTransaction(encryptedAmountReceiver, ivReceiver, TransactionType.DEPOSIT, senderWallet, receiverWallet, "Received from " + sender.getUsername());
        transactionRepository.save(receiverTransaction);

        // Now create the hash and sign for receiver transaction
        String receiverHash = hashingService.generateTransactionHash(senderWallet.getWalletID().toString(), receiverWallet.getWalletID().toString(), amount.toString(), receiverTransaction.getTimestamp());
        receiverTransaction.setHashValueOfTransaction(receiverHash);
        PrivateKey receiverPrivateKey = decryptUserPrivateKey(receiver, receiverKEK);
        byte[] receiverSignature = signTransactionHash(receiverHash, receiverPrivateKey);
        receiverTransaction.setDigitalSignature(receiverSignature); // Updated to handle byte[] for digitalSignature
        transactionRepository.save(receiverTransaction);

        session.setAttribute("balance", senderWallet.getBalance());
    }
}
