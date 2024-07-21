package com.fyp14.fyp;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

@RestController
public class RequestController {
    @GetMapping("/")
    public String textReturn() {
        return "This is my backend";
    }

    //http://localhost:8080/transferRequest?amount=27.23&receiver=josh
    @GetMapping("/transferRequest")
    public String transferRequest(@RequestParam("amount") float a, @RequestParam("receiver") String receiver) {
        return String.format("Transfer request to %s for S$%.2f", receiver, a);
    }

    //http://localhost:8080/TestPage.html?amount=213&name=Josh
    @GetMapping("/TestPage")
    public RedirectView testDemo(@RequestParam("amount") float a, @RequestParam("receiver") String receiver) {
        System.out.printf("Transfer request to %s for S$%.2f%n", receiver, a);

        String redirectUrl = String.format("/transferRequest?amount=%.2f&receiver=%s", a, receiver);
        return new RedirectView(redirectUrl);
    }

    //Test Push
}
