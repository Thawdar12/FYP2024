package com.fyp14.OnePay.FDS;

import com.fyp14.OnePay.KeyManagement.KEK.KeyManagementService;
import com.fyp14.OnePay.Mail.MailService;
import com.fyp14.OnePay.Transcation.*;
import com.fyp14.OnePay.User.User;
import com.fyp14.OnePay.User.UserRepository;
import com.fyp14.OnePay.Wallet.Wallet;
import com.fyp14.OnePay.Wallet.WalletRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class Fds {
    private final KeyManagementService keyManagementService; // Add KeyManagementService
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final MailService mailService;
    private final TransactionConfirmationRepository transactionConfirmationRepository;

    public Fds(KeyManagementService keyManagementService, TransactionRepository transactionRepository,
               UserRepository userRepository, WalletRepository walletRepository,
               MailService mailService, TransactionConfirmationRepository transactionConfirmationRepository) {
        this.keyManagementService = keyManagementService;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.mailService = mailService;
        this.transactionConfirmationRepository = transactionConfirmationRepository;
    }

    public void fdsComputation(Transaction senderTransaction, Transaction receiverTransaction, BigDecimal rawAmount, User sender, User receiver) throws Exception {
        // Step 0: Retrieve historical benchmark (Historical Threshold)
        Double historyBenchmark = sender.getUserBenchmark(); // Ensure this returns a Double

        // Initialize variables for adaptive threshold and weighting
        double adaptiveThreshold;
        double adaptiveWeight;
        double historicalWeight;

        // Step 1: Retrieve past transactions and compute features
        List<Transaction> transactionList = transactionRepository.findTransferTransactionsByFromWallet(sender.getWallet().getWalletID());

        // Decrypt amounts and collect transaction probabilities
        List<Double> pastProbabilities = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal sumOfAmounts = BigDecimal.ZERO;
        List<LocalDateTime> timestamps = new ArrayList<>();

        SecretKey masterKEK = keyManagementService.getMasterKEKFromEnv();
        SecretKey userKEK = keyManagementService.decryptUserKEK(sender.getEncryptedKEK(), sender.getKekEncryptionIV(), masterKEK);

        for (Transaction transaction : transactionList) {
            // Decrypt the amount
            String decryptedAmountStr = keyManagementService.decryptSensitiveData(transaction.getAmountEncrypted(), userKEK, transaction.getIv());
            BigDecimal amount = new BigDecimal(decryptedAmountStr);

            // Accumulate total amount and sum of amounts
            totalAmount = totalAmount.add(amount);
            sumOfAmounts = sumOfAmounts.add(amount);

            // Collect timestamps
            timestamps.add(transaction.getTimestamp());

            // Collect past probabilities if stored
            if (transaction.getProbability() != null) {
                pastProbabilities.add(transaction.getProbability());
            }
        }

        int numTransactions = transactionList.size();

        // Compute Average Amount
        BigDecimal averageAmount = numTransactions > 0 ? sumOfAmounts.divide(BigDecimal.valueOf(numTransactions), RoundingMode.HALF_UP) : BigDecimal.ZERO;

        // Compute Average Time Gap
        double averageTimeGap = 0.0;
        if (timestamps.size() >= 2) {
            // Sort timestamps
            timestamps.sort(Comparator.naturalOrder());

            // Compute time gaps
            List<Long> timeGaps = new ArrayList<>();
            for (int i = 1; i < timestamps.size(); i++) {
                long gap = Duration.between(timestamps.get(i - 1), timestamps.get(i)).toDays();
                timeGaps.add(gap);
            }

            // Compute average time gap
            long sumTimeGaps = timeGaps.stream().mapToLong(Long::longValue).sum();
            averageTimeGap = (double) sumTimeGaps / timeGaps.size();
        }

        // Step 2: Compute Nonlinear Features (Interaction Term)
        BigDecimal interactionTerm = averageAmount.multiply(BigDecimal.valueOf(averageTimeGap));

        // Step 3: Calculate Probability using Logistic Regression
        // Adjusted Coefficients for Lower Sensitivity
        double B0 = -5.0;
        double B1 = 0.001;  // Reduced from 0.005
        double B2 = 0.005;  // Reduced from 0.02
        double B3 = 0.2;    // Reduced from 0.5
        double B4 = 0.0005; // Reduced from 0.001

        // Optionally, scale features
        double scaledTotalAmount = totalAmount.doubleValue() / 10000; // Example scaling
        double scaledAverageAmount = averageAmount.doubleValue() / 1000; // Example scaling

        double linearCombination = B0 + B1 * scaledTotalAmount + B2 * scaledAverageAmount + B3 * averageTimeGap + B4 * interactionTerm.doubleValue();
        double probability = 1 / (1 + Math.exp(-linearCombination));

        // Step 4: Adjust Probability based on special rules
        // Reduced adjustments for lower sensitivity
        if (rawAmount.compareTo(BigDecimal.valueOf(5000)) > 0) {
            probability += 0.05; // Reduced from 0.09
        }
        if (rawAmount.compareTo(BigDecimal.valueOf(20000)) > 0) {
            // Check if the account is within the first week
            Duration accountAge = Duration.between(sender.getCreated_at(), LocalDateTime.now());
            if (accountAge.toDays() <= 7) {
                probability += 0.15; // Reduced from 0.3
            }
        }

        // Cap probability between 0 and 1
        probability = Math.min(1.0, probability);

        // Step 5: Compute Thresholds
        // Historical Threshold
        double historicalThreshold = 0.0;
        if (pastProbabilities.size() >= 2) {
            double meanProbability = pastProbabilities.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double stdDevProbability = calculateStdDev(pastProbabilities, meanProbability);
            historicalThreshold = meanProbability + 3 * stdDevProbability;
        } else {
            // Not enough data; set historical threshold to a default low value
            historicalThreshold = 0.5;
        }

        // Adaptive Threshold (for recent transactions)
        adaptiveThreshold = calculateAdaptiveThreshold(sender);

        // Step 6: Determine weights based on user history
        if (historyBenchmark == null || historyBenchmark.equals(0.0)) {
            // New user; rely more on adaptive threshold
            adaptiveWeight = 0.7; // Reduced from 0.8
            historicalWeight = 0.3; // Increased from 0.2
        } else {
            adaptiveWeight = 0.6; // Reduced from 0.5
            historicalWeight = 0.4; // Increased from 0.5
        }

        // Combine thresholds with a buffer
        double combinedThreshold = (adaptiveWeight * adaptiveThreshold) + (historicalWeight * historicalThreshold); // Added 0.1 buffer

        // Step 7: Apply Exceptions and Special Rules
        // High-Value Transaction for new accounts
        Duration accountAge = Duration.between(sender.getCreated_at(), LocalDateTime.now());
        if (rawAmount.compareTo(BigDecimal.valueOf(2000)) >= 0 && accountAge.toDays() < 1) {
            // **Set the probability before flagging as suspicious**
            senderTransaction.setProbability(probability);
            receiverTransaction.setProbability(probability);

            // Flag as suspicious
            senderTransaction.setStatus(TransactionStatus.SUSPICIOUS);
            receiverTransaction.setStatus(TransactionStatus.SUSPICIOUS);

            // Save transactions
            transactionRepository.save(senderTransaction);
            transactionRepository.save(receiverTransaction);

            // Update userBenchmark
            updateUserBenchmark(sender);

            Wallet senderWallet = sender.getWallet();
            senderWallet.setBalance(senderWallet.getBalance().subtract(rawAmount));

            sendTransactionConfirmationEmail(sender, senderTransaction, receiverTransaction);

            return;
        }

        // Step 8: Final Decision
        if (probability >= combinedThreshold || combinedThreshold - probability <= 0.001) {
            // Flag as suspicious
            senderTransaction.setStatus(TransactionStatus.SUSPICIOUS);
            receiverTransaction.setStatus(TransactionStatus.SUSPICIOUS);

            Wallet senderWallet = sender.getWallet();
            senderWallet.setBalance(senderWallet.getBalance().subtract(rawAmount));

            System.out.println("Probability: " + probability);
            System.out.println("Combined Threshold: " + combinedThreshold);

            sendTransactionConfirmationEmail(sender, senderTransaction, receiverTransaction);
        } else {
            // Mark as not suspicious and proceed with the transaction
            Wallet senderWallet = sender.getWallet();
            Wallet receiverWallet = receiver.getWallet();
            senderWallet.setBalance(senderWallet.getBalance().subtract(rawAmount));
            receiverWallet.setBalance(receiverWallet.getBalance().add(rawAmount));
            walletRepository.save(senderWallet);
            walletRepository.save(receiverWallet);
            senderTransaction.setStatus(TransactionStatus.COMPLETED);
            receiverTransaction.setStatus(TransactionStatus.COMPLETED);
            System.out.println("Probability: " + probability);
            System.out.println("Combined Threshold: " + combinedThreshold);
        }

        // **Set the probability for all transactions before saving**
        senderTransaction.setProbability(probability);
        receiverTransaction.setProbability(probability);

        // Save transactions
        transactionRepository.save(senderTransaction);
        transactionRepository.save(receiverTransaction);

        // Step 9: Update userBenchmark based on new transaction
        updateUserBenchmark(sender);
    }

    private void updateUserBenchmark(User sender) throws Exception {
        // Retrieve all transfer transactions for the user
        List<Transaction> transactionList = transactionRepository.findTransferTransactionsByFromWallet(sender.getWallet().getWalletID());

        // Collect all non-null probabilities
        List<Double> probabilities = new ArrayList<>();
        for (Transaction transaction : transactionList) {
            if (transaction.getProbability() != null) {
                probabilities.add(transaction.getProbability());
            }
        }

        if (probabilities.isEmpty()) {
            // No transactions to compute benchmark
            sender.setUserBenchmark(null);
        } else {
            // Compute mean and standard deviation
            double mean = probabilities.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double stdDev = calculateStdDev(probabilities, mean);

            // Compute historical benchmark as mean + 3 * stdDev
            double historicalBenchmark = mean + 3 * stdDev;

            // Update userBenchmark
            sender.setUserBenchmark(historicalBenchmark);
        }

        // Save the updated user
        userRepository.save(sender);
    }

    private double calculateStdDev(List<Double> values, double mean) {
        double variance = 0.0;
        for (double value : values) {
            variance += Math.pow(value - mean, 2);
        }
        variance /= values.size();
        return Math.sqrt(variance);
    }

    private double calculateAdaptiveThreshold(User sender) throws Exception {
        // Number of recent transactions to consider
        int recentTransactionCount = 5;

        // Retrieve the user's recent transactions
        Pageable pageable = PageRequest.of(0, recentTransactionCount);
        List<Transaction> recentTransactions = transactionRepository.findTopNTransferTransactionsByFromWallet(
                sender.getWallet().getWalletID(),
                pageable).getContent();

        // Check if we have enough transactions
        if (recentTransactions.size() >= 2) {
            // Collect probabilities from recent transactions
            List<Double> recentProbabilities = new ArrayList<>();

            for (Transaction transaction : recentTransactions) {
                // Ensure the transaction has a stored probability
                if (transaction.getProbability() != null) {
                    recentProbabilities.add(transaction.getProbability());
                }
            }

            // Ensure we have at least 2 probabilities to compute statistics
            if (recentProbabilities.size() >= 2) {
                // Compute mean and standard deviation
                double meanProbability = recentProbabilities.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                double stdDevProbability = calculateStdDev(recentProbabilities, meanProbability);

                // Compute adaptive threshold
                double k = 1.0; // Sensitivity constant
                double adaptiveThreshold = meanProbability + k * stdDevProbability;

                // Ensure the threshold does not exceed 1
                adaptiveThreshold = Math.min(1.0, adaptiveThreshold);

                return adaptiveThreshold;
            }
        }

        // Not enough data; set adaptive threshold to a default value
        return 0.6;
    }

    private void sendTransactionConfirmationEmail(User user, Transaction senderTransaction, Transaction receiverTransaction) throws Exception {
        // Generate a unique token
        String token = UUID.randomUUID().toString();

        // Create and save TransactionConfirmation
        TransactionConfirmation confirmation = new TransactionConfirmation();
        confirmation.setToken(token);
        confirmation.setTransactions(List.of(senderTransaction, receiverTransaction));
        confirmation.setCreatedAt(LocalDateTime.now());
        transactionConfirmationRepository.save(confirmation);

        // Use MailService to send the email
        mailService.sendTransactionConfirmationEmail(user, token);
    }
}
