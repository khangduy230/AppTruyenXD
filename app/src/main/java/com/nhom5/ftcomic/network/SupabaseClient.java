package com.nhom5.ftcomic.network;

import android.content.Context;

import com.nhom5.ftcomic.utils.SessionManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SupabaseClient {

    private static Retrofit publicRetrofit;

    public static Retrofit getRetrofit() {
        if (publicRetrofit == null) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {
                        Request originalRequest = chain.request();

                        Request newRequest = originalRequest.newBuilder()
                                .addHeader("apikey", SupabaseConfig.API_KEY)
                                .addHeader("Authorization", "Bearer " + SupabaseConfig.API_KEY)
                                .addHeader("Content-Type", "application/json")
                                .build();

                        return chain.proceed(newRequest);
                    })
                    .addInterceptor(loggingInterceptor)
                    .build();

            publicRetrofit = new Retrofit.Builder()
                    .baseUrl(SupabaseConfig.BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }

        return publicRetrofit;
    }

    public static Retrofit getRetrofit(Context context) {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        SessionManager sessionManager = new SessionManager(context.getApplicationContext());

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request originalRequest = chain.request();

                    String accessToken = sessionManager.getAccessToken();
                    String bearerToken;

                    if (accessToken != null && !accessToken.trim().isEmpty()) {
                        bearerToken = accessToken;
                    } else {
                        bearerToken = SupabaseConfig.API_KEY;
                    }

                    Request newRequest = originalRequest.newBuilder()
                            .addHeader("apikey", SupabaseConfig.API_KEY)
                            .addHeader("Authorization", "Bearer " + bearerToken)
                            .addHeader("Content-Type", "application/json")
                            .build();

                    return chain.proceed(newRequest);
                })
                .addInterceptor(loggingInterceptor)
                .build();

        return new Retrofit.Builder()
                .baseUrl(SupabaseConfig.BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    public static SupabaseApi getApi() {
        return getRetrofit().create(SupabaseApi.class);
    }

    public static SupabaseApi getApi(Context context) {
        return getRetrofit(context).create(SupabaseApi.class);
    }
}