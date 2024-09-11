package com.fyp14.OnePay.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RedirectForGetRequestController {

    @GetMapping({"/", "/OnePay/home"})
    public String showHomePage() {
        return "/OnePay/home"; // This resolves to 'templates/OnePay/home.html'
    }

    @GetMapping("/OnePay/signUp")
    public String showSignUpPage() {
        return "/OnePay/signUp"; // This resolves to 'templates/OnePay/signUp.html'
    }

    // Add mapping for the custom login page  /////
    @GetMapping("/OnePay/singIn")
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


}
