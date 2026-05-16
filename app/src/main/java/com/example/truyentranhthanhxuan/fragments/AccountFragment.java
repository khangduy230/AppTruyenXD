package com.example.truyentranhthanhxuan.fragments;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.truyentranhthanhxuan.R;
import com.example.truyentranhthanhxuan.activities.SettingsActivity;

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

        //khi bấm vào truyện đã tải thì hiện popup login_fragment
        TextView btnDownload = view.findViewById(R.id.btn_download);
        btnDownload.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Tạo một đối tượng LoginFragment
                    LoginFragment loginFragment = new LoginFragment();
                    // Hiển thị pop-up
                    loginFragment.show(getParentFragmentManager(), loginFragment.getTag());
                }
            });

        //trả về view
        return view;
    }
}