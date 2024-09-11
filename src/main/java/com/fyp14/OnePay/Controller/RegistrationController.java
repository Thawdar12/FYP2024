package com.fyp14.OnePay.Controller;

import com.fyp14.OnePay.User.User;
import com.fyp14.OnePay.User.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RegistrationController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }


    //first we resolve request to signup page to the correct html page by using PostMapping
    //next we save the request body into a user object and into our database
    //finally, when save the user into our database, we're going to save an encrypted password
    @PostMapping(value = "/OnePay/signUp", consumes = "application/json")
    public User createUser(@RequestBody User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }
}