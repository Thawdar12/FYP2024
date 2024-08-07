package com.fyp14.OnePay.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RedirectForGetRequestController {

    @GetMapping({"/", "/OnePay/home"})
    public String showHomePage() {
        return "OnePay/home"; // This resolves to 'templates/OnePay/home.html'
    }

    @GetMapping("/OnePay/signUp")
    public String showSignUpPage() {
        return "OnePay/signUp"; // This resolves to 'templates/OnePay/signUp.html'
    }

    @GetMapping("/OnePay/dashboard")
    public String showDashboardPage() {
        return "OnePay/dashboard"; // This resolves to 'templates/OnePay/dashboard.html'
    }

}
