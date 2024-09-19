//this controller handle wallet related request,
//which is topup and transfer

package com.fyp14.OnePay.Controller;

import com.fyp14.OnePay.User.User;
import com.fyp14.OnePay.User.UserRepository;
import com.fyp14.OnePay.Wallet.Wallet;
import com.fyp14.OnePay.Wallet.WalletRepository;
import com.fyp14.OnePay.Wallet.WalletService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/OnePay")
public class WalletController {

    private final WalletService walletService;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    public WalletController(WalletService walletService, UserRepository userRepository, WalletRepository walletRepository) {
        this.walletService = walletService;
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
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

    private Map<String, Object> createResponse(String status, String message, BigDecimal balance) {
        Map<String, Object> res = new HashMap<>();
        res.put("status", status);
        res.put("message", message);
        res.put("balance", balance);
        return res;
    }
}
