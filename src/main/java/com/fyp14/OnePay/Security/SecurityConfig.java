//this config file handle redirect request before login
//as we don't want all user to access certain page without login

package com.fyp14.OnePay.Security;

import com.fyp14.OnePay.Wallet.WalletRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final PasswordEncoder passwordEncoder;
    private final UserDetailsService userDetailsService;
    private final WalletRepository walletRepository;

    public SecurityConfig(PasswordEncoder passwordEncoder, UserDetailsService userDetailsService, WalletRepository walletRepository) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.walletRepository = walletRepository;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationSuccessHandler customAuthenticationSuccessHandler() {
        return new CustomAuthenticationSuccessHandler(walletRepository);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)  // CSRF is disabled for development environment
                .formLogin(httpForm -> {
                    httpForm
                            .loginPage("/OnePay/home").permitAll()  // dedicated login page
                            .loginProcessingUrl("/login")           // Endpoint for login processing
                            .successHandler(customAuthenticationSuccessHandler())  // Redirect on successful login
                            .failureUrl("/OnePay/404");   // Redirect on login failure, this is for testing, to be change later
                })
                .logout(logout -> {
                    logout
                            .logoutUrl("/OnePay/dashboard/logout")   // URL to trigger logout
                            .logoutSuccessUrl("/OnePay/home")        // Redirect on successful logout
                            .invalidateHttpSession(true)             // Invalidate the HTTP session
                            .deleteCookies("JSESSIONID") // Delete cookies
                            .permitAll();                            // Allow all to access logout URL
                })
                .authorizeHttpRequests(registry -> {
                    registry
                            .requestMatchers("/assets/**", "/css/**", "/js/**", "/images/**").permitAll()
                            .requestMatchers("/", "/OnePay/home", "/OnePay/signUp").permitAll()
                            .anyRequest().authenticated();
                });
        return http.build();
    }
}
