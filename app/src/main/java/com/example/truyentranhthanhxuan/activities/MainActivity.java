package com.example.truyentranhthanhxuan.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.truyentranhthanhxuan.R;
import com.example.truyentranhthanhxuan.fragments.AccountFragment;
import com.example.truyentranhthanhxuan.fragments.HomeFragment;
import com.example.truyentranhthanhxuan.fragments.LibraryFragment;
import com.example.truyentranhthanhxuan.fragments.SearchFragment;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //hiển thanh điều hướng dưới cùng
        setContentView(R.layout.activity_main);
        //gọi màn hình trang chủ
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    //thay thế khung fragment trống = home fragment
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }
        //ánh xạ các textview ở thanh điều hướng vào các biến
        TextView navHome = findViewById(R.id.nav_home);
        TextView navSearch = findViewById(R.id.nav_search);
        TextView navLibrary = findViewById(R.id.nav_library);
        TextView navProfile = findViewById(R.id.nav_profile);
        //sự kiện click của các textview
        navHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new HomeFragment())
                        .commit();
            }
        });
        navSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new SearchFragment())
                        .commit();
            }
        });
        navLibrary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new LibraryFragment())
                        .commit();
            }
        });
        navProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new AccountFragment())
                        .commit();
            }
        });
        //

    }
}