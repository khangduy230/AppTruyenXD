package com.example.truyentranhthanhxuan;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class AccountFragment extends Fragment {

    public AccountFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //gán giao diện vào một biến 'view'
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        //tìm view theo id
        View btnSettings = view.findViewById(R.id.btn_settings);

        //bắt sự kiện OnClickListener (khi click vào btnSettings)
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //chuyển từ AccountFragment -> SettingsActivity
                Intent intent = new Intent(getActivity(), SettingsActivity.class);
                startActivity(intent);
            }
        });
        //trả về view
        return view;
    }
}