//after user successfully authenticated
//this handler inject certain information into session

package com.fyp14.OnePay.Security;

import com.fyp14.OnePay.User.CustomUserDetails;
import com.fyp14.OnePay.Wallet.WalletRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.math.BigDecimal;

public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final WalletRepository walletRepository;

    public CustomAuthenticationSuccessHandler(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        // Get the authenticated user details
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        // Retrieve userID and walletID
        Long userID = userDetails.getUserID();
        Long walletID = userDetails.getWalletID();
        String username = userDetails.getUsername();

        // Retrieve wallet balance using walletID
        BigDecimal balance = walletRepository.findById(walletID)
                .map(wallet -> wallet.getBalance())
                .orElse(BigDecimal.ZERO); // Default to 0 if wallet not found

        // Set attributes in the session
        HttpSession session = request.getSession();
        session.setAttribute("userID", userID);
        session.setAttribute("username", username);
        session.setAttribute("walletID", walletID);
        session.setAttribute("balance", balance); // Store balance in session

        // Redirect to the default success URL
        response.sendRedirect("/OnePay/dashboard/index");
    }
}
