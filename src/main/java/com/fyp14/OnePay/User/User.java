package com.fyp14.OnePay.User;

import jakarta.persistence.*;

// The user database table
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "userID", nullable = false)
    private Long userID;
    private String username;
    private String email;
    private String password;
    @Enumerated(EnumType.STRING)
    private UserRole userRole;
    private Boolean locked;
    private Boolean enabled;
    private int phoneNumber;

    // Default constructor required by JPA and JSon
    public User() {
        this.userRole = UserRole.USER; // Default role
        this.locked = false;           // Default locked status
        this.enabled = true;           // Default enabled status
    }

    // Constructor for full initialization
    public User(String username, String email, String password, UserRole userRole, Boolean locked, Boolean enabled, int phoneNumber) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.userRole = userRole;
        this.locked = locked;
        this.enabled = enabled;
        this.phoneNumber = phoneNumber;
    }

    // Constructor for minimal initialization
    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Getters and setters
    public Long getUserID() {
        return userID;
    }

    public void setUserID(Long userID) {
        this.userID = userID;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public UserRole getUserRole() {
        return userRole;
    }

    public void setUserRole(UserRole userRole) {
        this.userRole = userRole;
    }

    public Boolean getLocked() {
        return locked;
    }

    public void setLocked(Boolean locked) {
        this.locked = locked;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public int getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(int phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
