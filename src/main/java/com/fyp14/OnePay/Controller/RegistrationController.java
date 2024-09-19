//this controller handle registration request endpoint

package com.fyp14.OnePay.Controller;

import com.fyp14.OnePay.User.User;
import com.fyp14.OnePay.User.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<String> createUser(@RequestBody User user) throws Exception {
        userService.registerUser(user);
        return new ResponseEntity<>("User registered successfully", HttpStatus.CREATED);
    }
}