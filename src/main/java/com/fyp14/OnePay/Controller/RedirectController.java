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

    @GetMapping("/OnePay/dashboard/top-up")
    public String getTopUpPage() {
        return "OnePay/dashboard/topup"; // This resolves to 'templates/OnePay/dashboard/topup.html'
    }

    @GetMapping("/OnePay/dashboard/withdraw")
    public String getWithdrawPage() {
        return "OnePay/dashboard/withdraw"; // This resolves to 'templates/OnePay/dashboard/withdraw.html'
    }

    @GetMapping("/OnePay/dashboard/wallet-details")
    public String getWalletDetailsPage() {
        return "OnePay/dashboard/wallet-details"; // This resolves to 'templates/OnePay/dashboard/withdraw.html'
    }

    @GetMapping("/OnePay/dashboard/history")
    public String getWalletHistoryPage() {
        return "OnePay/dashboard/history"; // This resolves to 'templates/OnePay/dashboard/wallet/history.html'
    }

    @GetMapping("/OnePay/dashboard/moneytransfer")
    public String getTransferPage() {
        return "OnePay/dashboard/moneytransfer"; // This resolves to 'templates/OnePay/dashboard/transfer/moneytransfer.html'
    }

    @GetMapping("/OnePay/dashboard/transfer-details")
    public String getTransferDetailsPage() {
        return "OnePay/dashboard/transfer-details"; // This resolves to 'templates/OnePay/dashboard/transfer/transfer-details.html'
    }

    @GetMapping("/OnePay/dashboard/wallet-history")
    public String getWalletHistory() {
        return "OnePay/dashboard/wallet-history"; // This resolves to 'templates/OnePay/dashboard/transfer/wallet-history.html'
    }

    @GetMapping("/OnePay/dashboard/account-details")
    public String getAccountDetailsPage() {
        return "OnePay/dashboard/account-details"; // This resolves to 'templates/OnePay/dashboard/transfer/account-details.html'
    }

}
