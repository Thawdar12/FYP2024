//this controller handle registration request endpoint

package com.fyp14.OnePay.Controller;

import com.fyp14.OnePay.User.User;
import com.fyp14.OnePay.User.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class RegistrationController {

    private UserService userService;

    public RegistrationController(UserService userService) {
        this.userService = userService;
    }

    //first we resolve request to signup page to the correct html page by using PostMapping
    //next we save the request body into a user object and into our database, via UserService
    //finally, when save the user into our database, we're going to save an encrypted password
    @PostMapping(value = "/OnePay/signUp", consumes = "application/json")
    public ResponseEntity<Map<String, String>> createUser(@RequestBody User user) {
        Map<String, String> response = new HashMap<>();
        try {
            userService.registerUser(user);
            response.put("message", "User registered successfully. Please check your email to activate your account.");
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", "An error occurred while registering the user");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}


