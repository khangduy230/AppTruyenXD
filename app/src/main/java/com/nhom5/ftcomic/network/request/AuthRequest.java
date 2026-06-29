package com.nhom5.ftcomic.network.request;

import java.util.HashMap;
import java.util.Map;

public class AuthRequest {

    private String email;
    private String password;
    private SignUpOptions options;

    public AuthRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public AuthRequest(String email, String password, String securityQuestion, String securityAnswer) {
        this.email = email;
        this.password = password;
        this.options = new SignUpOptions(securityQuestion, securityAnswer);
    }

    public static class SignUpOptions {
        private Map<String, String> data;

        public SignUpOptions(String securityQuestion, String securityAnswer) {
            this.data = new HashMap<>();
            this.data.put("security_question", securityQuestion);
            this.data.put("security_answer", securityAnswer);
        }
    }
}