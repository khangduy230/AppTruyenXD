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

    // Dùng chung 1 client, không tạo mới mỗi lần fetch
    private static final OkHttpClient client = new OkHttpClient();

    private View               layoutUserInfo;
    private TextView           tvUserName, tvUserEmail;
    private TextView           btnLogin, btnLogout, btnDownload, btnHistory, btnManage, btnManageUsers;
    private View               btnSettings;
    private ShapeableImageView ivAvatar;
    private SessionManager     sessionManager;

    public AccountFragment() {}

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

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
        // Mỗi lần quay lại fragment: cập nhật UI từ session local trước,
        // rồi fetch mới từ server để đồng bộ
        updateUI();
        fetchProfileAndUpdateUI();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Setup
    // ─────────────────────────────────────────────────────────────────────────

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

    // ─────────────────────────────────────────────────────────────────────────
    // Network
    // ─────────────────────────────────────────────────────────────────────────

    private void fetchProfileAndUpdateUI() {
        if (!sessionManager.isLoggedIn()) return;

        String userId      = sessionManager.getUserId();
        String accessToken = sessionManager.getAccessToken();

        Request request = new Request.Builder()
                .url(SupabaseConfig.PROJECT_URL
                        + "rest/v1/profiles?id=eq." + userId
                        + "&select=username,avatar_url")
                .get()
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("apikey", SupabaseConfig.API_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Giữ nguyên dữ liệu session cũ, không làm gì thêm
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // Luôn đọc và đóng body để tránh connection leak
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

                    // Cập nhật UI trên main thread sau khi đã lưu session mới
                    if (getActivity() == null || getActivity().isFinishing()) return;
                    getActivity().runOnUiThread(() -> updateUI());

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UI
    // ─────────────────────────────────────────────────────────────────────────

    private void updateUI() {
        boolean isLogged = sessionManager.isLoggedIn();

        if (layoutUserInfo != null) layoutUserInfo.setVisibility(isLogged ? View.VISIBLE : View.GONE);
        if (btnLogout != null)      btnLogout.setVisibility(isLogged ? View.VISIBLE : View.GONE);
        if (btnLogin != null)       btnLogin.setVisibility(isLogged ? View.GONE : View.VISIBLE);
        if (btnManageUsers != null) btnManageUsers.setVisibility(isLogged ? View.VISIBLE : View.GONE);

        if (!isLogged) {
            if (tvUserEmail != null) tvUserEmail.setText("");
            if (tvUserName != null)  tvUserName.setText("");
            return;
        }

        // Email
        if (tvUserEmail != null) {
            tvUserEmail.setText(sessionManager.getEmail());
        }

        // Username
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

        // Avatar
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

    /**
     * Load ảnh từ URL với cache-bust timestamp.
     * Tránh Glide hiển thị ảnh cũ khi URL giống nhau nhưng file đã thay đổi.
     */
    private void loadAvatarFromUrl(String url) {
        if (getActivity() == null || getActivity().isFinishing() || !isAdded()) return;

        // Xóa timestamp cũ nếu có, rồi thêm mới
        String baseUrl      = url.contains("?") ? url.substring(0, url.indexOf('?')) : url;
        String cacheBustUrl = baseUrl + "?t=" + System.currentTimeMillis();

        Glide.with(this)
                .load(cacheBustUrl)
                .circleCrop()
                .diskCacheStrategy(DiskCacheStrategy.NONE)  // không cache trên disk
                .skipMemoryCache(true)                       // không cache trên memory
                .placeholder(R.drawable.ic_profile)
                .into(ivAvatar);

        ivAvatar.setImageTintList(null);
    }
}