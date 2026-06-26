package com.nhom5.ftcomic.fragments;

import com.nhom5.ftcomic.database.AppDatabase;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.imageview.ShapeableImageView;
import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.activities.DownloadedActivity;
import com.nhom5.ftcomic.activities.ManageComicsActivity;
import com.nhom5.ftcomic.activities.ReadingHistoryActivity;
import com.nhom5.ftcomic.activities.SettingsActivity;
import com.nhom5.ftcomic.activities.UserProfileActivity;
import com.nhom5.ftcomic.activities.ManageUsersActivity;
import com.nhom5.ftcomic.network.SupabaseConfig;
import com.nhom5.ftcomic.utils.AuthHelper;
import com.nhom5.ftcomic.utils.SessionManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class AccountFragment extends Fragment {

    private static final OkHttpClient client = new OkHttpClient();

    private View               layoutUserInfo;
    private TextView           tvUserName, tvUserEmail;
    private TextView           btnLogin, btnLogout, btnDownload, btnHistory, btnManage, btnManageUsers;
    private View               btnSettings;
    private ShapeableImageView ivAvatar;
    private SessionManager     sessionManager;

    public AccountFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);
        sessionManager = new SessionManager(requireContext());

        initViews(view);
        setupClickListeners();
        setupFragmentResultListener();

        updateUI();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sessionManager == null) return;
        updateUI();
        fetchProfileAndUpdateUI();
    }

    private void initViews(View view) {
        layoutUserInfo = view.findViewById(R.id.layout_user_info);
        tvUserName     = view.findViewById(R.id.tvUserName);
        tvUserEmail    = view.findViewById(R.id.tvUserEmail);
        btnLogin       = view.findViewById(R.id.btn_login);
        btnLogout      = view.findViewById(R.id.btn_logout);
        btnDownload    = view.findViewById(R.id.btn_download);
        btnSettings    = view.findViewById(R.id.btn_settings);
        btnHistory     = view.findViewById(R.id.btn_history);
        btnManage      = view.findViewById(R.id.btn_manage);
        btnManageUsers = view.findViewById(R.id.btn_manage_users);
        ivAvatar       = view.findViewById(R.id.iv_avatar);
    }

    private void setupClickListeners() {
        btnSettings.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), SettingsActivity.class))
        );

        btnDownload.setOnClickListener(v ->
                AuthHelper.KiemTraVaThucHien(getContext(), getParentFragmentManager(), () ->
                        startActivity(new Intent(getActivity(), DownloadedActivity.class))
                )
        );

        if (btnHistory != null) {
            btnHistory.setOnClickListener(v ->
                    startActivity(new Intent(getActivity(), ReadingHistoryActivity.class))
            );
        }

        if (btnManage != null) {
            btnManage.setOnClickListener(v ->
                    AuthHelper.KiemTraVaThucHien(getContext(), getParentFragmentManager(), () ->
                            startActivity(new Intent(getActivity(), ManageComicsActivity.class))
                    )
            );
        }

        if (btnManageUsers != null) {
            btnManageUsers.setOnClickListener(v ->
                    AuthHelper.KiemTraVaThucHien(getContext(), getParentFragmentManager(), () ->
                            startActivity(new Intent(getActivity(), ManageUsersActivity.class))
                    )
            );
        }

        btnLogin.setOnClickListener(v ->
                new LoginFragment().show(getParentFragmentManager(), "LoginFragment")
        );

        btnLogout.setOnClickListener(v -> {
            String oldUserId = sessionManager.getUserId();
            sessionManager.logout();

            AppDatabase db = AppDatabase.getInstance(requireContext());
            AppDatabase.databaseWriteExecutor.execute(() -> {
                if (oldUserId != null && !oldUserId.trim().isEmpty()) {
                    db.favoriteDao().deleteFavoritesByUser(oldUserId);
                    db.readingHistoryDao().deleteHistoriesByUser(oldUserId);
                    db.ratingDao().deleteRatingsByUser(oldUserId);
                }
            });

            Toast.makeText(getContext(), "Đã đăng xuất thành công!", Toast.LENGTH_SHORT).show();
            updateUI();
        });

        layoutUserInfo.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), UserProfileActivity.class))
        );
    }

    private void setupFragmentResultListener() {
        getParentFragmentManager().setFragmentResultListener(
                "key_dang_nhap",
                getViewLifecycleOwner(),
                (requestKey, result) -> {
                    updateUI();
                    fetchProfileAndUpdateUI();
                }
        );
    }

    private void fetchProfileAndUpdateUI() {
        if (!sessionManager.isLoggedIn()) return;

        String userId      = sessionManager.getUserId();
        String accessToken = sessionManager.getAccessToken();

        Request request = new Request.Builder()
                .url(SupabaseConfig.PROJECT_URL
                        + "rest/v1/profiles?id=eq." + userId
                        + "&select=username,avatar_url,role")
                .get()
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("apikey", SupabaseConfig.API_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String json;
                try (ResponseBody rb = response.body()) {
                    if (!response.isSuccessful() || rb == null) return;
                    json = rb.string();
                }

                if (json.isEmpty() || json.equals("[]")) return;

                try {
                    JSONArray array = new JSONArray(json);
                    if (array.length() == 0) return;

                    JSONObject profile = array.getJSONObject(0);

                    if (!profile.isNull("username")) {
                        sessionManager.saveUsername(profile.getString("username"));
                    }
                    if (!profile.isNull("avatar_url")) {
                        sessionManager.saveAvatarUri(profile.getString("avatar_url"));
                    }
                    if (!profile.isNull("role")) {
                        sessionManager.saveRole(profile.getString("role"));
                    }

                    if (getActivity() == null || getActivity().isFinishing()) return;
                    getActivity().runOnUiThread(() -> updateUI());

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void updateUI() {
        boolean isLogged = sessionManager.isLoggedIn();

        if (layoutUserInfo != null) layoutUserInfo.setVisibility(isLogged ? View.VISIBLE : View.GONE);
        if (btnLogout != null)      btnLogout.setVisibility(isLogged ? View.VISIBLE : View.GONE);
        if (btnLogin != null)       btnLogin.setVisibility(isLogged ? View.GONE : View.VISIBLE);

        if (!isLogged) {
            if (tvUserEmail != null) tvUserEmail.setText("");
            if (tvUserName != null)  tvUserName.setText("");
            if (btnManage != null)      btnManage.setVisibility(View.GONE);
            if (btnManageUsers != null) btnManageUsers.setVisibility(View.GONE);
            return;
        }

        String role = sessionManager.getRole();
        if ("admin".equals(role)) {
            if (btnManage != null)      btnManage.setVisibility(View.VISIBLE);
            if (btnManageUsers != null) btnManageUsers.setVisibility(View.VISIBLE);
        } else if ("translator".equals(role)) {
            if (btnManage != null)      btnManage.setVisibility(View.VISIBLE);
            if (btnManageUsers != null) btnManageUsers.setVisibility(View.GONE);
        } else {
            if (btnManage != null)      btnManage.setVisibility(View.GONE);
            if (btnManageUsers != null) btnManageUsers.setVisibility(View.GONE);
        }

        if (tvUserEmail != null) {
            tvUserEmail.setText(sessionManager.getEmail());
        }

        if (tvUserName != null) {
            String username = sessionManager.getUsername();
            if (username != null && !username.isEmpty()) {
                tvUserName.setText(username);
            } else {
                String email = sessionManager.getEmail();
                tvUserName.setText((email != null && email.contains("@"))
                        ? email.substring(0, email.indexOf('@'))
                        : "Người dùng");
            }
        }

        if (ivAvatar != null) {
            String savedUri = sessionManager.getAvatarUri();
            if (savedUri != null && !savedUri.isEmpty()) {
                if (savedUri.startsWith("http")) {
                    loadAvatarFromUrl(savedUri);
                } else {
                    ivAvatar.setImageURI(Uri.parse(savedUri));
                    ivAvatar.setImageTintList(null);
                }
            }
        }
    }

    private void loadAvatarFromUrl(String url) {
        if (getActivity() == null || getActivity().isFinishing() || !isAdded()) return;

        String baseUrl      = url.contains("?") ? url.substring(0, url.indexOf('?')) : url;
        String cacheBustUrl = baseUrl + "?t=" + System.currentTimeMillis();

        Glide.with(this)
                .load(cacheBustUrl)
                .circleCrop()
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .placeholder(R.drawable.ic_profile)
                .into(ivAvatar);

        ivAvatar.setImageTintList(null);
    }
}