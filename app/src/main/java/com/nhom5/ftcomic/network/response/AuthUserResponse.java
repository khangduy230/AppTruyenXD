package com.nhom5.ftcomic.network.response;

import com.google.gson.annotations.SerializedName;

public class AuthUserResponse {

    @SerializedName("id")
    private String id;

    @SerializedName("email")
    private String email;

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }
}