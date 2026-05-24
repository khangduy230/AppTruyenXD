package com.nhom5.ftcomic.network;

import android.util.Log;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SupabaseAuthClient {

    private static Retrofit retrofit;

    public static Retrofit getRetrofit() {
        if (retrofit == null) {

            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message -> {
                Log.d("AUTH_OKHTTP", message);
            });
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

            retrofit = new Retrofit.Builder()
                    .baseUrl(SupabaseConfig.AUTH_BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }

        return retrofit;
    }

    public static SupabaseAuthApi getApi() {
        return getRetrofit().create(SupabaseAuthApi.class);
    }
}