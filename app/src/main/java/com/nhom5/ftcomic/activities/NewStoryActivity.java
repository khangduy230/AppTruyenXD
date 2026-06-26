package com.nhom5.ftcomic.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.network.SupabaseClient;
import com.nhom5.ftcomic.network.SupabaseConfig;
import com.nhom5.ftcomic.utils.SessionManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NewStoryActivity extends AppCompatActivity {

    private static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json");
    private static final MediaType MEDIA_TYPE_IMAGE = MediaType.parse("image/jpeg");

    private TextInputEditText edtNewTitle, edtNewDescription, edtNewAuthor, edtNewTranslator;
    private MaterialCheckBox cbIsTranslator;
    private ChipGroup chipGroupNewGenres;
    private ImageView imgNewCover;
    private TextView tvSelectCoverHint;
    private MaterialButton btnCreateComic;

    private SessionManager sessionManager;
    private final OkHttpClient client = new OkHttpClient();
    private Uri selectedImageUri = null;
    private String myUsername = "FTComic";

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    imgNewCover.setImageURI(uri);
                    tvSelectCoverHint.setVisibility(View.GONE);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_new_story);

        sessionManager = new SessionManager(this);

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        initViews();
        setupToolbar();
        setupClickListeners();

        cbIsTranslator.setChecked(true);
        fetchMyProfileName();
    }

    private void initViews() {
        edtNewTitle = findViewById(R.id.edtNewTitle);
        edtNewDescription = findViewById(R.id.edtNewDescription);
        edtNewAuthor = findViewById(R.id.edtNewAuthor);
        edtNewTranslator = findViewById(R.id.edtNewTranslator);
        cbIsTranslator = findViewById(R.id.cbIsTranslator);
        chipGroupNewGenres = findViewById(R.id.chipGroupNewGenres);
        imgNewCover = findViewById(R.id.imgNewCover);
        tvSelectCoverHint = findViewById(R.id.tvSelectCoverHint);
        btnCreateComic = findViewById(R.id.btnCreateComic);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void setupClickListeners() {
        findViewById(R.id.cardNewCover).setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        btnCreateComic.setOnClickListener(v -> onCreateComicClicked());

        cbIsTranslator.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                edtNewTranslator.setText(myUsername);
                edtNewTranslator.setEnabled(false);
            } else {
                edtNewTranslator.setText("");
                edtNewTranslator.setEnabled(true);
            }
        });

        View btnAddNewGenre = findViewById(R.id.btnAddNewGenre);
        if (btnAddNewGenre != null) {
            btnAddNewGenre.setOnClickListener(v -> showAddCategoryBottomSheet());
        }
    }

    private void fetchMyProfileName() {
        String userId = sessionManager.getUserId();
        if (userId == null) return;

        Request request = new Request.Builder()
                .url(SupabaseConfig.PROJECT_URL + "/rest/v1/profiles?id=eq." + userId + "&select=username")
                .get()
                .addHeader("Authorization", "Bearer " + sessionManager.getAccessToken())
                .addHeader("apikey", SupabaseConfig.API_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray array = new JSONArray(response.body().string());
                        if (array.length() > 0) {
                            myUsername = array.getJSONObject(0).getString("username");
                            runOnUiThread(() -> {
                                if (cbIsTranslator.isChecked()) edtNewTranslator.setText(myUsername);
                            });
                        }
                    } catch (Exception ignored) {}
                }
            }
        });
    }

    private void showAddCategoryBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.item_add_category, null);
        bottomSheetDialog.setContentView(view);

        ChipGroup chipGroupGenreSelection = view.findViewById(R.id.chipGroupGenreSelection);
        MaterialButton btnConfirmCategory = view.findViewById(R.id.btnConfirmCategory);

        Request request = new Request.Builder()
                .url(SupabaseConfig.PROJECT_URL + "/rest/v1/categories?select=*")
                .get()
                .addHeader("Authorization", "Bearer " + sessionManager.getAccessToken())
                .addHeader("apikey", SupabaseConfig.API_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray array = new JSONArray(response.body().string());
                        runOnUiThread(() -> {
                            for (int i = 0; i < array.length(); i++) {
                                try {
                                    JSONObject obj = array.getJSONObject(i);
                                    int id = obj.getInt("id");
                                    String name = obj.getString("name");

                                    Chip chip = new Chip(NewStoryActivity.this);
                                    chip.setText(name);
                                    chip.setTag(id);
                                    chip.setCheckable(true);

                                    for (int j = 0; j < chipGroupNewGenres.getChildCount(); j++) {
                                        View c = chipGroupNewGenres.getChildAt(j);
                                        if (c instanceof Chip && name.equals(((Chip) c).getText().toString())) {
                                            chip.setChecked(true);
                                        }
                                    }
                                    chipGroupGenreSelection.addView(chip);
                                } catch (Exception ignored) {}
                            }
                        });
                    } catch (Exception ignored) {}
                }
            }
        });

        btnConfirmCategory.setOnClickListener(v -> {
            for (int i = chipGroupNewGenres.getChildCount() - 1; i >= 0; i--) {
                View child = chipGroupNewGenres.getChildAt(i);
                if (child.getId() != R.id.btnAddNewGenre) {
                    chipGroupNewGenres.removeView(child);
                }
            }

            for (int i = 0; i < chipGroupGenreSelection.getChildCount(); i++) {
                View vc = chipGroupGenreSelection.getChildAt(i);
                if (vc instanceof Chip && ((Chip) vc).isChecked()) {
                    Chip mainChip = new Chip(NewStoryActivity.this);
                    mainChip.setText(((Chip) vc).getText().toString());
                    mainChip.setTag(vc.getTag());
                    mainChip.setCloseIconVisible(true);
                    mainChip.setOnCloseIconClickListener(view1 -> chipGroupNewGenres.removeView(mainChip));
                    chipGroupNewGenres.addView(mainChip, 0);
                }
            }
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    private void onCreateComicClicked() {
        String title = edtNewTitle.getText().toString().trim();
        String desc = edtNewDescription.getText().toString().trim();
        String author = edtNewAuthor.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "Tên truyện không được để trống!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedImageUri == null) {
            Toast.makeText(this, "Vui lòng chọn ảnh bìa truyện!", Toast.LENGTH_SHORT).show();
            return;
        }

        btnCreateComic.setEnabled(false);

        JSONObject body = new JSONObject();
        try {
            body.put("name", title);
            body.put("description", desc.isEmpty() ? "Chưa có mô tả." : desc);
            body.put("author", author.isEmpty() ? "Đang cập nhật" : author);
            body.put("uploader_id", sessionManager.getUserId());
            body.put("status", "Đang ra");
            body.put("section", "all");
        } catch (JSONException e) {
            btnCreateComic.setEnabled(true);
            return;
        }

        Request request = new Request.Builder()
                .url(SupabaseConfig.PROJECT_URL + "/rest/v1/comics")
                .post(RequestBody.create(body.toString(), MEDIA_TYPE_JSON))
                .addHeader("Authorization", "Bearer " + sessionManager.getAccessToken())
                .addHeader("apikey", SupabaseConfig.API_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    btnCreateComic.setEnabled(true);
                    Toast.makeText(NewStoryActivity.this, "Lỗi kết nối khi tạo truyện", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray array = new JSONArray(response.body().string());
                        int insertedId = array.getJSONObject(0).getInt("id");
                        uploadCoverAndFinalize(insertedId);
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            btnCreateComic.setEnabled(true);
                            Toast.makeText(NewStoryActivity.this, "Lỗi xử lý phản hồi dữ liệu", Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        btnCreateComic.setEnabled(true);
                        Toast.makeText(NewStoryActivity.this, "Lỗi khởi tạo hàng bảng comics: " + response.code(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void uploadCoverAndFinalize(int newComicId) {
        byte[] imageBytes;
        try {
            InputStream is = getContentResolver().openInputStream(selectedImageUri);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int n;
            while ((n = is.read(buf)) != -1) bos.write(buf, 0, n);
            imageBytes = bos.toByteArray();
        } catch (IOException e) {
            runOnUiThread(() -> Toast.makeText(NewStoryActivity.this, "Lỗi đọc tệp tin hình ảnh", Toast.LENGTH_SHORT).show());
            saveComicCategories(newComicId); // Dự phòng chuyển tiếp lưu thể loại
            return;
        }

        String fileName = "comic_cover_" + newComicId + ".jpg";

        // Điều chỉnh URL trỏ trực tiếp vào bucket comics-storage và thư mục con /covers/
        Request request = new Request.Builder()
                .url(SupabaseConfig.PROJECT_URL + "/storage/v1/object/" + SupabaseConfig.COMICS_BUCKET + "/" + fileName)
                .post(RequestBody.create(imageBytes, MEDIA_TYPE_IMAGE))
                .addHeader("Authorization", "Bearer " + sessionManager.getAccessToken())
                .addHeader("apikey", SupabaseConfig.API_KEY)
                .addHeader("x-upsert", "true")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                saveComicCategories(newComicId);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String finalCoverUrl = SupabaseConfig.STORAGE_PUBLIC_URL + SupabaseConfig.COMICS_BUCKET + "/" + fileName;
                    updateComicCoverUrl(newComicId, finalCoverUrl);
                } else {
                    runOnUiThread(() -> Toast.makeText(NewStoryActivity.this, "Lỗi Storage: " + response.code(), Toast.LENGTH_SHORT).show());
                    saveComicCategories(newComicId);
                }
            }
        });
    }

    private void updateComicCoverUrl(int id, String coverUrl) {
        JSONObject body = new JSONObject();
        try {
            body.put("cover_url", coverUrl);
        } catch (JSONException ignored) {}

        Request request = new Request.Builder()
                .url(SupabaseConfig.PROJECT_URL + "/rest/v1/comics?id=eq." + id)
                .patch(RequestBody.create(body.toString(), MEDIA_TYPE_JSON))
                .addHeader("Authorization", "Bearer " + sessionManager.getAccessToken())
                .addHeader("apikey", SupabaseConfig.API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                saveComicCategories(id);
            }

            @Override
            public void onResponse(Call call, Response response) {
                saveComicCategories(id);
            }
        });
    }

    private void saveComicCategories(int id) {
        JSONArray bodyArray = new JSONArray();
        for (int i = 0; i < chipGroupNewGenres.getChildCount(); i++) {
            View view = chipGroupNewGenres.getChildAt(i);
            if (view instanceof Chip && view.getId() != R.id.btnAddNewGenre) {
                if (view.getTag() != null) {
                    try {
                        JSONObject row = new JSONObject();
                        row.put("comic_id", id);
                        row.put("category_id", (int) view.getTag());
                        bodyArray.put(row);
                    } catch (Exception ignored) {}
                }
            }
        }

        if (bodyArray.length() == 0) {
            completeCreation();
            return;
        }

        Request insertRequest = new Request.Builder()
                .url(SupabaseConfig.PROJECT_URL + "/rest/v1/comic_categories")
                .post(RequestBody.create(bodyArray.toString(), MEDIA_TYPE_JSON))
                .addHeader("Authorization", "Bearer " + sessionManager.getAccessToken())
                .addHeader("apikey", SupabaseConfig.API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(insertRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                completeCreation();
            }

            @Override
            public void onResponse(Call call, Response response) {
                completeCreation();
            }
        });
    }

    private void completeCreation() {
        runOnUiThread(() -> {
            btnCreateComic.setEnabled(true);
            Toast.makeText(NewStoryActivity.this, "Đăng tải bộ truyện mới thành công!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}