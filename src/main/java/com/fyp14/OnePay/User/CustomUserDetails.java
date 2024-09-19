//this code is used to set information into session
//as session user userDetails, which is a Spring Boots class,
//and we implement it so that we can set custom variable into it.

package com.fyp14.OnePay.User;

import com.fyp14.OnePay.Wallet.Wallet;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {

    private final User user;
    private final Wallet wallet;

    public CustomUserDetails(User user, Wallet wallet) {
        this.user = user;
        this.wallet = wallet;
    }

    public Long getUserID() {
        return user.getUserID();
    }

    public Long getWalletID() {
        return wallet.getWalletID();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonLocked() {
        return !user.getLocked();
    }

    @Override
    public boolean isEnabled() {
        return user.getEnabled();
    }
}