//this controller handle wallet related request,
//which is topup and transfer

package com.fyp14.OnePay.Controller;

import com.fyp14.OnePay.KeyManagement.KEK.KeyManagementService;
import com.fyp14.OnePay.Transcation.Transaction;
import com.fyp14.OnePay.Transcation.TransactionDTO;
import com.fyp14.OnePay.Transcation.TransactionRepository;
import com.fyp14.OnePay.Transcation.TransactionType;
import com.fyp14.OnePay.User.User;
import com.fyp14.OnePay.User.UserRepository;
import com.fyp14.OnePay.Wallet.Wallet;
import com.fyp14.OnePay.Wallet.WalletRepository;
import com.fyp14.OnePay.Wallet.WalletService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/OnePay")
public class WalletController {

    private final WalletService walletService;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final KeyManagementService keyManagementService;

    public WalletController(WalletService walletService, UserRepository userRepository, WalletRepository walletRepository, TransactionRepository transactionRepository, KeyManagementService keyManagementService) {
        this.walletService = walletService;
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.keyManagementService = keyManagementService;
    }

    @PostMapping("/topup")
    public ResponseEntity<?> topUpWallet(@RequestBody Map<String, Object> payload, HttpSession session) {
        //1. test if amount is more than 0.
        try {
            BigDecimal amount = new BigDecimal(payload.get("amount").toString());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body(createResponse("fail", "Invalid amount", null));
            }

            //grab userID and walletID from session information
            Long userID = (Long) session.getAttribute("userID");
            Long walletID = (Long) session.getAttribute("walletID");

            //if failed to get any ID, which means session is not authorize
            if (userID == null || walletID == null) {
                return ResponseEntity.badRequest().body(createResponse("fail", "User is not authenticated", null));
            }

            //search database if user exist
            User user = userRepository.findById(userID).orElse(null);
            Wallet wallet = walletRepository.findById(walletID).orElse(null);

            if (user == null || wallet == null) {
                return ResponseEntity.badRequest().body(createResponse("fail", "User or wallet not found", null));
            }

            // Perform the top-up, via walletService
            walletService.topUpWallet(user, wallet, amount, session); // Updated to pass session

            // Return updated balance in the response
            BigDecimal updatedBalance = wallet.getBalance();
            return ResponseEntity.ok(createResponse("success", "Top-up successful", updatedBalance));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(createResponse("fail", e.getMessage(), null));
        }
    }

    @PostMapping("/transfer")
    public ResponseEntity<?> transferMoney(@RequestBody Map<String, Object> payload, HttpSession session) {
        try {
            String receiverPhoneNumber = payload.get("receiverMobileNumber").toString();
            BigDecimal amount = new BigDecimal(payload.get("amount").toString());

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body(createResponse("fail", "Invalid transfer amount", null));
            }

            Long userID = (Long) session.getAttribute("userID");
            if (userID == null) {
                return ResponseEntity.badRequest().body(createResponse("fail", "User is not authenticated", null));
            }

            User sender = userRepository.findById(userID).orElse(null);
            if (sender == null || sender.getWallet() == null) {
                return ResponseEntity.badRequest().body(createResponse("fail", "Sender's wallet not found", null));
            }

            // Perform the transfer (delegating to WalletService)
            walletService.transferMoney(session, receiverPhoneNumber, amount);

            // Return updated balance in the response
            Wallet senderWallet = sender.getWallet();
            BigDecimal updatedBalance = senderWallet.getBalance();
            return ResponseEntity.ok(createResponse("success", "Transfer successful", updatedBalance));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(createResponse("fail", e.getMessage(), null));
        }
    }

    @GetMapping("/fetchHistory")
    public ResponseEntity<?> fetchHistory(HttpSession session) {
        try {
            Long userID = (Long) session.getAttribute("userID");
            if (userID == null) {
                return ResponseEntity.badRequest().body("User is not authenticated");
            }

            // Retrieve the user
            User user = userRepository.findById(userID).orElseThrow(() -> new Exception("User not found"));

            // Initialize KeyManagementService
            SecretKey masterKEK = keyManagementService.getMasterKEKFromEnv();
            SecretKey userKEK = keyManagementService.decryptUserKEK(user.getEncryptedKEK(), user.getKekEncryptionIV(), masterKEK);

            // Fetch and filter transactions as before...
            List<Transaction> transactions = transactionRepository.findByUserId(userID);
            List<Transaction> filteredTransactions = transactions.stream()
                    .filter(t -> {
                        TransactionType type = t.getTransactionType();
                        Long fromWalletID = t.getFromWallet() != null ? t.getFromWallet().getWalletID() : null;
                        Long toWalletID = t.getToWallet() != null ? t.getToWallet().getWalletID() : null;

                        boolean isDeposit = type == TransactionType.DEPOSIT && userID.equals(toWalletID);
                        boolean isTransferSent = type == TransactionType.TRANSFER && userID.equals(fromWalletID);

                        return isDeposit || isTransferSent;
                    })
                    .collect(Collectors.toList());

            // Map transactions to DTOs with decrypted amounts
            List<TransactionDTO> transactionDTOs = filteredTransactions.stream()
                    .map(t -> {
                        try {
                            String decryptedAmount = keyManagementService.decryptSensitiveData(t.getAmountEncrypted(), userKEK, t.getIv());
                            return new TransactionDTO(t, decryptedAmount);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return null; // Handle exception as needed
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(transactionDTOs);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching the transaction history.");
        }
    }

    @PostMapping("/withdraw")
    public ResponseEntity<?> withdrawFromWallet(@RequestBody Map<String, Object> payload, HttpSession session) {
        try {
            //1. test if amount is more than 0.
            BigDecimal amount = new BigDecimal(payload.get("amount").toString());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body(createResponse("fail", "Invalid amount", null));
            }

            //grab userID and walletID from session information
            Long userID = (Long) session.getAttribute("userID");
            Long walletID = (Long) session.getAttribute("walletID");

            //if failed to get any ID, which means session is not authorize
            if (userID == null || walletID == null) {
                return ResponseEntity.badRequest().body(createResponse("fail", "User is not authenticated", null));
            }

            //search database if user exist
            User user = userRepository.findById(userID).orElse(null);
            Wallet wallet = walletRepository.findById(walletID).orElse(null);

            if (user == null || wallet == null) {
                return ResponseEntity.badRequest().body(createResponse("fail", "User or wallet not found", null));
            }

            // Check if the wallet has sufficient balance for withdrawal
            if (wallet.getBalance().compareTo(amount) < 0) {
                return ResponseEntity.badRequest().body(createResponse("fail", "Insufficient balance", null));
            }

            // Perform the withdrawal via WalletService
            walletService.withdrawFromWallet(user, wallet, amount, session);

            // Return updated balance in the response
            BigDecimal updatedBalance = wallet.getBalance();
            return ResponseEntity.ok(createResponse("success", "Withdrawal successful", updatedBalance));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(createResponse("fail", e.getMessage(), null));
        }
    }


    private Map<String, Object> createResponse(String status, String message, Object data) {
        Map<String, Object> res = new HashMap<>();
        res.put("status", status);
        res.put("message", message);
        res.put("data", data);
        return res;
    }
}
