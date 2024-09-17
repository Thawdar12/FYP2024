package com.fyp14.OnePay.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RedirectForGetRequestController {

    @GetMapping({"/", "/OnePay/home"})
    public String showHomePage() {
        return "/OnePay/home"; // This resolves to 'templatxes/OnePay/home.html'
    }

    @GetMapping("/OnePay/signUp")
    public String showSignUpPage() {
        return "/OnePay/signUp"; // This resolves to 'templates/OnePay/signUp.html'
    }

    // Add mapping for the custom login page  /////
    @GetMapping("/OnePay/singin")
    public String showLoginPage() {
        return "/OnePay/dashboard/page-login"; // This resolves to 'templates/page-login.html'
    }

    // Handle logout page if required    ///////
    @GetMapping("/OnePay/logout")
    public String logoutPage() {
        return "redirect:/OnePay/home"; // After logging out, redirect to the home page
    }

    @GetMapping("/OnePay/dashboard/index")
    public String showDashboardPage() {
        return "/OnePay/dashboard/index"; // This resolves to 'templates/OnePay/dashboard/index.html'
    }


    @GetMapping("/OnePay/dashboard/transfer")
    public String getTransferPage() {
        return "/OnePay/dashboard/transfer"; // This resolves to 'templates/OnePay/dashboard/transfer.html'
    }

    @GetMapping("/OnePay/dashboard/initiateTransfer")
    public String getInitiateTransferPage() {
        return "/OnePay/dashboard/initiateTransfer"; // This resolves to 'templates/OnePay/dashboard/initiateTransfer.html'
    }

    @GetMapping("/OnePay/dashboard/topup")
    public String getTopUpPage() {
        return "/OnePay/dashboard/topup"; // This resolves to 'templates/OnePay/dashboard/topup.html'
    }

    @GetMapping("/OnePay/dashboard/createWallet")
    public String getCreateWalletPage() {
        return "/OnePay/dashboard/create-wallet"; // This resolves to 'templates/OnePay/dashboard/createWallet.html'
    }
}
