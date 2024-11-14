//this controller does following job:
//1. redirect get request to point at the index dashboard page
//2. grab information set in session, such as userID or walletID
//and load them for thymeleaf engine, this is done everytime you reload this page (dashboard page)

package com.fyp14.OnePay.Controller;

import com.fyp14.OnePay.User.User;
import com.fyp14.OnePay.User.UserRepository;
import com.fyp14.OnePay.Wallet.Wallet;
import com.fyp14.OnePay.Wallet.WalletRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;

@Controller
public class DashboardController {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    public DashboardController(UserRepository userRepository, WalletRepository walletRepository) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
    }

    @GetMapping("/OnePay/dashboard/index")
    public String dashboard(HttpSession session, Model model) {
        // Retrieve userID from session
        Long userID = (Long) session.getAttribute("userID");

        if (userID != null) {
            // Fetch the user and wallet from the database
            User user = userRepository.findById(userID).orElse(null);
            if (user != null) {
                Wallet wallet = user.getWallet();
                BigDecimal balance = wallet != null ? wallet.getBalance() : BigDecimal.ZERO;

                // Add user data to the Thymeleaf model, model is where thymeleaf engine grab variable from
                model.addAttribute("username", user.getUsername());
                model.addAttribute("balance", balance);
                session.setAttribute("email", user.getEmail());
                session.setAttribute("phoneNumber", user.getPhoneNumber());
            } else {
                // Handle case where user is not found
                model.addAttribute("username", "Guest");
                model.addAttribute("balance", BigDecimal.ZERO);
            }
        } else {
            // Handle unauthenticated access or session expiration
            model.addAttribute("username", "Guest");
            model.addAttribute("balance", BigDecimal.ZERO);
        }

        // Return the dashboard template
        return "OnePay/dashboard/index"; // This resolves to 'templates/OnePay/dashboard/index.html'
    }
}
