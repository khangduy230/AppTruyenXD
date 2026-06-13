package com.nhom5.ftcomic.network;

import android.content.Context;

import com.nhom5.ftcomic.utils.SessionManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SupabaseClient {

    private static Retrofit retrofit;
    private static Context appContext;

    public static SupabaseApi getApi(Context context) {
        appContext = context.getApplicationContext();
        return getRetrofit().create(SupabaseApi.class);
    }

    // Giữ lại hàm cũ để những chỗ lấy dữ liệu public vẫn không lỗi
    public static SupabaseApi getApi() {
        return getRetrofit().create(SupabaseApi.class);
    }

    private static Retrofit getRetrofit() {
        if (retrofit == null) {

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {
                        String bearerToken = SupabaseConfig.API_KEY;

                        if (appContext != null) {
                            SessionManager sessionManager = new SessionManager(appContext);
                            String accessToken = sessionManager.getAccessToken();

                            if (accessToken != null && !accessToken.trim().isEmpty()) {
                                bearerToken = accessToken;
                            }
                        }

                        Request request = chain.request().newBuilder()
                                .addHeader("apikey", SupabaseConfig.API_KEY)
                                .addHeader("Authorization", "Bearer " + bearerToken)
                                .addHeader("Content-Type", "application/json")
                                .build();

                        return chain.proceed(request);
                    })
                    .addInterceptor(logging)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(SupabaseConfig.BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }

        return retrofit;
    }
}