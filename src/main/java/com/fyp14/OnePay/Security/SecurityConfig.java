package com.fyp14.OnePay.Security;

import com.fyp14.OnePay.User.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity

public class SecurityConfig {

    private final UserService userService;

    public SecurityConfig(UserService userService) {
        this.userService = userService;
    }

    //we need to return UserService as UserDetailService
    @Bean
    public UserDetailsService userDetailsService() {
        return userService;
    }

    //Spring Boot default password encryption method
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    //this function authenticate user with database, when they try to log in
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService());
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)  //csrf is disabled for development environment
                .formLogin(httpForm -> {
                    httpForm
                            .loginPage("/OnePay/home").permitAll()  // Custom login page
                            .loginProcessingUrl("/login")          // Endpoint for login processing
                            .defaultSuccessUrl("/OnePay/dashboard/index")  // Redirect on successful login
                            .failureUrl("/OnePay/home?error=true");   // Redirect on login failure
                })  //this override the default Spring Boot login form

                .authorizeHttpRequests(registry -> {
                    registry
                            .requestMatchers("/assets/**", "/css/**", "/js/**", "/images/**", "/dashboardassets/**").permitAll()
                            .requestMatchers("/", "/OnePay/home", "/OnePay/signUp").permitAll()
                            .anyRequest().authenticated();
                })  //this allows user to visit these page without login

                .build();
    }
}
