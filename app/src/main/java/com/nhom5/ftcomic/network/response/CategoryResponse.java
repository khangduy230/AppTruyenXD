package com.nhom5.ftcomic.network.response;

import com.google.gson.annotations.SerializedName;

public class CategoryResponse {

    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}