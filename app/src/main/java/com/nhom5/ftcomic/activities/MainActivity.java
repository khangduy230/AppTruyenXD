package com.nhom5.ftcomic.activities;

import androidx.core.splashscreen.SplashScreen;
import androidx.fragment.app.Fragment;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.nhom5.ftcomic.R;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.nhom5.ftcomic.fragments.AccountFragment;
import com.nhom5.ftcomic.fragments.HomeFragment;
import com.nhom5.ftcomic.fragments.LibraryFragment;
import com.nhom5.ftcomic.fragments.SearchFragment;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        //hiển thanh điều hướng dưới cùng
        setContentView(R.layout.activity_main);
        //ánh xạ view theo id đã đặt trong layout
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        //gọi màn hình trang chủ
        if (savedInstanceState == null) {
            replaceFragment(new HomeFragment());
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }
        //khi click vào item
        bottomNavigationView.setOnItemSelectedListener(item -> {
            //lấy id của item
            int itemId = item.getItemId();
            //kiểm tra item được chọn
            if (itemId == R.id.nav_home) {
                //thay thế fragment
                replaceFragment(new HomeFragment());
                return true;
            } else if (itemId == R.id.nav_search) {
                replaceFragment(new SearchFragment());
                return true;
            } else if (itemId == R.id.nav_library) {
                replaceFragment(new LibraryFragment());
                return true;
            } else if (itemId == R.id.nav_profile) {
                replaceFragment(new AccountFragment());
                return true;
            }
            return false;
        });

    }
    private void replaceFragment(Fragment fragment){
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.commit();
    }
}