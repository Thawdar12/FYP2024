//This file handles all redirect request
//which means if you JUST want to make something clickable
//and redirect to another page
//do it here.

package com.fyp14.OnePay.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RedirectController {

    @GetMapping({"/", "/OnePay/home"})
    public String showHomePage() {
        return "OnePay/home"; // This resolves to 'templates/OnePay/home.html'
    }

    @GetMapping("/OnePay/dashboard/transfer")
    public String getTransferPage() {
        return "OnePay/dashboard/transfer"; // This resolves to 'templates/OnePay/dashboard/transfer.html'
    }

    @GetMapping("/OnePay/dashboard/top-up")
    public String getTopUpPage() {
        return "OnePay/dashboard/topup"; // This resolves to 'templates/OnePay/dashboard/topup.html'
    }
}