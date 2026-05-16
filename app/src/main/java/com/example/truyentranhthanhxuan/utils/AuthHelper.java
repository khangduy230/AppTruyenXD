package com.example.truyentranhthanhxuan.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.fragment.app.FragmentManager;

import com.example.truyentranhthanhxuan.fragments.LoginFragment;

public class AuthHelper {
    public static void KiemTraVaThucHien(Context context, FragmentManager fragmentManager, Runnable hanhDongNeuDaDangNhap) {

        SharedPreferences sharedPreferences = context.getSharedPreferences("my_application", Context.MODE_PRIVATE);
        boolean isLogged = sharedPreferences.getBoolean("DaDangNhap", false);

        if (isLogged) {
            hanhDongNeuDaDangNhap.run();
        } else {
            LoginFragment loginFragment = new LoginFragment();
            loginFragment.show(fragmentManager, loginFragment.getTag());
        }
    }
}
