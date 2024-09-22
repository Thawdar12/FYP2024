package com.fyp14.OnePay.KeyManagement.KEK;

import javax.crypto.SecretKey;

public class UserKeyData {
    private Long userID;
    private SecretKey userKEK;
    private byte[] privateKey;

    // Constructor
    public UserKeyData(Long userID, SecretKey userKEK, byte[] privateKey) {
        this.userID = userID;
        this.userKEK = userKEK;
        this.privateKey = privateKey;
    }

    // Getters and setters
    public Long getUserID() {
        return userID;
    }

    public void setUserID(Long userID) {
        this.userID = userID;
    }

    public SecretKey getUserKEK() {
        return userKEK;
    }

    public void setUserKEK(SecretKey userKEK) {
        this.userKEK = userKEK;
    }

    public byte[] getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(byte[] privateKey) {
        this.privateKey = privateKey;
    }
}
