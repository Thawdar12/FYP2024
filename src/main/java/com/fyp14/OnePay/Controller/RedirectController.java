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

    // Dashboard
    @GetMapping("/OnePay/dashboard/top-up")
    public String getTopUpPage() {
        return "OnePay/dashboard/topup"; // This resolves to 'templates/OnePay/dashboard/topup.html'
    }

    @GetMapping("/OnePay/dashboard/withdraw")
    public String getWithdrawPage() {
        return "OnePay/dashboard/withdraw"; // This resolves to 'templates/OnePay/dashboard/withdraw.html'
    }

    // My Wallet
    @GetMapping("/OnePay/dashboard/wallet-details")
    public String getWalletDetailsPage() {
        return "OnePay/dashboard/wallet-details"; // This resolves to 'templates/OnePay/dashboard/wallet-details.html'
    }

    @GetMapping("/OnePay/dashboard/wallet-history")
    public String getWalletHistoryPage() {
        return "OnePay/dashboard/wallet-history"; // This resolves to 'templates/OnePay/dashboard/wallet-history.html'
    }


    // Transfer
    @GetMapping("/OnePay/dashboard/moneytransfer")
    public String getTransferPage() {
        return "OnePay/dashboard/moneytransfer"; // This resolves to 'templates/OnePay/dashboard/moneytransfer.html'
    }

    @GetMapping("/OnePay/dashboard/transfer-history")
    public String getTransferDetailsPage() {
        return "OnePay/dashboard/transfer-history"; // This resolves to 'templates/OnePay/dashboard/transfer-history.html'
    }


    // Account
    @GetMapping("/OnePay/dashboard/account-details")
    public String getAccountDetailsPage() {
        return "OnePay/dashboard/account-details"; // This resolves to 'templates/OnePay/dashboard/account-details.html'
    }

    @GetMapping("/OnePay/dashboard/settings")
    public String getSettingsPage() {
        return "OnePay/dashboard/settings"; // This resolves to 'templates/OnePay/dashboard/settings.html'
    }

    @GetMapping("/OnePay/dashboard/terms&conditions")
    public String getInvoicesPage() {
        return "OnePay/dashboard/terms&conditions"; // This resolves to 'templates/OnePay/dashboard/invoices.html'
    }

    @GetMapping("/OnePay/dashboard/activity-history")
    public String getActivityHistoryPage() {
        return "OnePay/dashboard/activity-history"; // This resolves to 'templates/OnePay/dashboard/activity-history.html'
    }


}
