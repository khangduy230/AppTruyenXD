package com.nhom5.ftcomic.network.request;

import com.google.gson.annotations.SerializedName;
import java.util.HashMap;
import java.util.Map;

public class AuthRequest {

    @SerializedName("email")
    private String email;

    @SerializedName("password")
    private String password;

    @SerializedName("data")
    private Map<String, String> data;

    @SerializedName("options")
    private SignUpOptions options;

    public AuthRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public AuthRequest(String email, String password, Map<String, String> metadata) {
        this.email = email;
        this.password = password;
        this.data = metadata;
        this.options = new SignUpOptions(metadata);
    }

    public static class SignUpOptions {
        @SerializedName("data")
        private Map<String, String> data;

        public SignUpOptions(Map<String, String> data) {
            this.data = data;
        }
    }
}