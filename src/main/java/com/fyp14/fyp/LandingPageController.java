package com.fyp14.fyp;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LandingPageController {
    @GetMapping("/")
    public String textReturn() {
        return "This is my backend";
    }
}
