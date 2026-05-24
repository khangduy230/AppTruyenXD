package com.nhom5.ftcomic.network.request;

public class AuthRequest {

    private String email;
    private String password;

    public AuthRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }
}