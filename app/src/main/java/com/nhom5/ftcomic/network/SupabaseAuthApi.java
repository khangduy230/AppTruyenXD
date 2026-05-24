package com.nhom5.ftcomic.network;

import com.nhom5.ftcomic.network.request.AuthRequest;
import com.nhom5.ftcomic.network.response.AuthResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface SupabaseAuthApi {

    @POST("signup")
    Call<AuthResponse> register(@Body AuthRequest request);

    @POST("token?grant_type=password")
    Call<AuthResponse> login(@Body AuthRequest request);
}