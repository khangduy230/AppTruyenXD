package com.nhom5.ftcomic.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.network.SupabaseClient;
import com.nhom5.ftcomic.network.SupabaseConfig;
import com.nhom5.ftcomic.network.response.ComicResponse;
import com.nhom5.ftcomic.utils.SessionManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class EditInformationActivity extends AppCompatActivity {

    private static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json");
    private static final MediaType MEDIA_TYPE_IMAGE = MediaType.parse("image/jpeg");

    private TextInputEditText edtStoryTitle, edtStoryDescription, edtAuthor, edtTranslator;
    private AutoCompleteTextView spinnerStatus;
    private MaterialSwitch switchHideComic;
    private ChipGroup chipGroupGenres;
    private ImageView imgCover;
    private FloatingActionButton btnDelete;
    private ExtendedFloatingActionButton btnSave;

    private String comicId;
    private SessionManager sessionManager;
    private final OkHttpClient client = new OkHttpClient();
    private Uri selectedImageUri = null;
    private String currentCoverUrl = null;
    private boolean isComicHidden = false;

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    imgCover.setImageURI(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_information);

        sessionManager = new SessionManager(this);
        comicId = getIntent().getStringExtra("COMIC_ID");

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
        setupDropdownMenu();
        setupClickListeners();
        fetchComicDetail();
    }

    private void initViews() {
        edtStoryTitle = findViewById(R.id.edtStoryTitle);
        edtStoryDescription = findViewById(R.id.edtStoryDescription);
        edtAuthor = findViewById(R.id.edtAuthor);
        edtTranslator = findViewById(R.id.edtTranslator);
        spinnerStatus = findViewById(R.id.spinnerStatus);
        switchHideComic = findViewById(R.id.switchHideComic);
        chipGroupGenres = findViewById(R.id.chipGroupGenres);
        imgCover = findViewById(R.id.imgCover);
        btnDelete = findViewById(R.id.btnDelete);
        btnSave = findViewById(R.id.btnSave);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void setupDropdownMenu() {
        String[] statusOptions = {"Đang ra", "Hoàn thành", "Tạm ngưng"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, statusOptions);
        spinnerStatus.setAdapter(adapter);
    }

    private void setupClickListeners() {
        findViewById(R.id.cardCover).setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        btnSave.setOnClickListener(v -> onSaveClicked());
        btnDelete.setOnClickListener(v -> showDeleteConfirmation());

        switchHideComic.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isComicHidden = isChecked;
            if (isChecked) {
                Toast.makeText(EditInformationActivity.this, "Truyện sẽ bị ẩn khỏi trang chủ", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(EditInformationActivity.this, "Truyện sẽ hiển thị công khai", Toast.LENGTH_SHORT).show();
            }
        });

        View btnAddGenre = findViewById(R.id.btnAddGenre);
        if (btnAddGenre != null) {
            btnAddGenre.setOnClickListener(v -> showAddCategoryBottomSheet());
        }
    }

    private void showAddCategoryBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.item_add_category, null);
        bottomSheetDialog.setContentView(view);

        ChipGroup chipGroupGenreSelection = view.findViewById(R.id.chipGroupGenreSelection);
        com.google.android.material.button.MaterialButton btnConfirmCategory = view.findViewById(R.id.btnConfirmCategory);

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
                        String json = response.body().string();
                        JSONArray array = new JSONArray(json);
                        runOnUiThread(() -> {
                            for (int i = 0; i < array.length(); i++) {
                                try {
                                    JSONObject obj = array.getJSONObject(i);
                                    int id = obj.getInt("id");
                                    String name = obj.getString("name");

                                    Chip chip = new Chip(EditInformationActivity.this);
                                    chip.setText(name);
                                    chip.setTag(id);
                                    chip.setCheckable(true);

                                    if (isGenreAlreadySelected(name)) {
                                        chip.setChecked(true);
                                    }

                                    chipGroupGenreSelection.addView(chip);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        btnConfirmCategory.setOnClickListener(v -> {
            for (int i = chipGroupGenres.getChildCount() - 1; i >= 0; i--) {
                View child = chipGroupGenres.getChildAt(i);
                if (child.getId() != R.id.btnAddGenre) {
                    chipGroupGenres.removeView(child);
                }
            }

            addSelectedChipsFromGroup(chipGroupGenreSelection);
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    private boolean isGenreAlreadySelected(String name) {
        for (int i = 0; i < chipGroupGenres.getChildCount(); i++) {
            View view = chipGroupGenres.getChildAt(i);
            if (view instanceof Chip && view.getId() != R.id.btnAddGenre) {
                Chip chip = (Chip) view;
                if (name.equals(chip.getText().toString())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void addSelectedChipsFromGroup(ChipGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View view = group.getChildAt(i);
            if (view instanceof Chip) {
                Chip bottomSheetChip = (Chip) view;
                if (bottomSheetChip.isChecked()) {
                    Chip mainChip = new Chip(EditInformationActivity.this);
                    mainChip.setText(bottomSheetChip.getText().toString());
                    mainChip.setTag(bottomSheetChip.getTag());
                    mainChip.setCloseIconVisible(true);
                    mainChip.setOnCloseIconClickListener(v -> chipGroupGenres.removeView(mainChip));
                    chipGroupGenres.addView(mainChip, 0);
                }
            }
        }
    }

    private void fetchComicDetail() {
        if (comicId == null) return;

        SupabaseClient.getApi(this).getComicById("eq." + comicId).enqueue(new retrofit2.Callback<List<ComicResponse>>() {
            @Override
            public void onResponse(retrofit2.Call<List<ComicResponse>> call, retrofit2.Response<List<ComicResponse>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    ComicResponse comic = response.body().get(0);
                    runOnUiThread(() -> populateData(comic));
                }
            }

            @Override
            public void onFailure(retrofit2.Call<List<ComicResponse>> call, Throwable t) {
                Toast.makeText(EditInformationActivity.this, "Lỗi nạp dữ liệu", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateData(ComicResponse comic) {
        edtStoryTitle.setText(comic.getName());
        edtStoryDescription.setText(comic.getDescription());
        edtAuthor.setText(comic.getAuthor());
        spinnerStatus.setText(comic.getStatus(), false);
        currentCoverUrl = comic.getCoverUrl();

        isComicHidden = "hidden".equals(comic.getSection());
        switchHideComic.setChecked(isComicHidden);

        Glide.with(this)
                .load(currentCoverUrl)
                .placeholder(R.drawable.ic_profile)
                .into(imgCover);

        fetchTranslatorName(comic.getUploaderId());
        fetchComicGenres();
    }

    private void fetchTranslatorName(String uploaderId) {
        if (uploaderId == null || uploaderId.trim().isEmpty()) {
            edtTranslator.setText("FTComic");
            return;
        }

        Request request = new Request.Builder()
                .url(SupabaseConfig.PROJECT_URL + "/rest/v1/profiles?id=eq." + uploaderId + "&select=username")
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
                        String json = response.body().string();
                        JSONArray array = new JSONArray(json);
                        if (array.length() > 0) {
                            String username = array.getJSONObject(0).getString("username");
                            runOnUiThread(() -> edtTranslator.setText(username));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void fetchComicGenres() {
        Request request = new Request.Builder()
                .url(SupabaseConfig.PROJECT_URL + "/rest/v1/comic_categories?comic_id=eq." + comicId + "&select=categories(id,name)")
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
                        String json = response.body().string();
                        JSONArray array = new JSONArray(json);
                        runOnUiThread(() -> {
                            for (int i = chipGroupGenres.getChildCount() - 1; i >= 0; i--) {
                                View child = chipGroupGenres.getChildAt(i);
                                if (child.getId() != R.id.btnAddGenre) {
                                    chipGroupGenres.removeView(child);
                                }
                            }

                            for (int i = 0; i < array.length(); i++) {
                                try {
                                    JSONObject obj = array.getJSONObject(i);
                                    if (obj.has("categories")) {
                                        JSONObject catObj = obj.getJSONObject("categories");
                                        int catId = catObj.getInt("id");
                                        String genreName = catObj.getString("name");

                                        Chip chip = new Chip(EditInformationActivity.this);
                                        chip.setText(genreName);
                                        chip.setTag(catId);
                                        chip.setCloseIconVisible(true);
                                        chip.setOnCloseIconClickListener(v -> chipGroupGenres.removeView(chip));
                                        chipGroupGenres.addView(chip, 0);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void onSaveClicked() {
        String title = edtStoryTitle.getText().toString().trim();
        String desc = edtStoryDescription.getText().toString().trim();
        String author = edtAuthor.getText().toString().trim();
        String status = spinnerStatus.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "Tên truyện không được để trống", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);
        if (selectedImageUri != null) {
            uploadCoverAndSaveData(title, desc, author, status);
        } else {
            updateComicProfile(title, desc, author, status, currentCoverUrl);
        }
    }

    private void uploadCoverAndSaveData(String title, String desc, String author, String status) {
        byte[] imageBytes;
        try {
            InputStream is = getContentResolver().openInputStream(selectedImageUri);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int n;
            while ((n = is.read(buf)) != -1) bos.write(buf, 0, n);
            imageBytes = bos.toByteArray();
        } catch (IOException e) {
            btnSave.setEnabled(true);
            return;
        }

        // ĐỒNG BỘ LOGIC: Tên file dạng comic_cover_[id].jpg
        String fileName = "comic_cover_" + comicId + ".jpg";
        String accessToken = sessionManager.getAccessToken();

        // ĐỒNG BỘ LOGIC: Đẩy trực tiếp vào gốc của COMICS_BUCKET thay vì AVATARS_BUCKET
        Request request = new Request.Builder()
                .url(SupabaseConfig.PROJECT_URL + "/storage/v1/object/" + SupabaseConfig.COMICS_BUCKET + "/" + fileName)
                .post(RequestBody.create(imageBytes, MEDIA_TYPE_IMAGE))
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("apikey", SupabaseConfig.API_KEY)
                .addHeader("x-upsert", "true")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    btnSave.setEnabled(true);
                    Toast.makeText(EditInformationActivity.this, "Lỗi kết nối máy chủ khi tải ảnh", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    // ĐỒNG BỘ LOGIC: Tạo URL trỏ trực tiếp đến file trong bucket comics-storage
                    String newCoverUrl = SupabaseConfig.STORAGE_PUBLIC_URL + SupabaseConfig.COMICS_BUCKET + "/" + fileName;
                    updateComicProfile(title, desc, author, status, newCoverUrl);
                } else {
                    runOnUiThread(() -> {
                        btnSave.setEnabled(true);
                        Toast.makeText(EditInformationActivity.this, "Lỗi up ảnh Storage: " + response.code(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void updateComicProfile(String title, String desc, String author, String status, String coverUrl) {
        JSONObject body = new JSONObject();
        try {
            body.put("name", title);
            body.put("description", desc);
            body.put("author", author);
            body.put("status", status);
            body.put("section", isComicHidden ? "hidden" : "all");
            if (coverUrl != null) body.put("cover_url", coverUrl);
        } catch (JSONException e) {
            btnSave.setEnabled(true);
            return;
        }

        String accessToken = sessionManager.getAccessToken();
        Request request = new Request.Builder()
                .url(SupabaseConfig.PROJECT_URL + "/rest/v1/comics?id=eq." + comicId)
                .patch(RequestBody.create(body.toString(), MEDIA_TYPE_JSON))
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("apikey", SupabaseConfig.API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> btnSave.setEnabled(true));
            }

            @Override
            public void onResponse(Call call, Response response) {
                if (response.isSuccessful()) {
                    saveComicCategories();
                } else {
                    runOnUiThread(() -> btnSave.setEnabled(true));
                }
            }
        });
    }

    private void saveComicCategories() {
        JSONArray bodyArray = new JSONArray();
        for (int i = 0; i < chipGroupGenres.getChildCount(); i++) {
            View view = chipGroupGenres.getChildAt(i);
            if (view instanceof Chip && view.getId() != R.id.btnAddGenre) {
                Chip chip = (Chip) view;
                if (chip.getTag() != null) {
                    try {
                        JSONObject row = new JSONObject();
                        row.put("comic_id", Integer.parseInt(comicId));
                        row.put("category_id", (int) chip.getTag());
                        bodyArray.put(row);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        String accessToken = sessionManager.getAccessToken();
        Request deleteRequest = new Request.Builder()
                .url(SupabaseConfig.PROJECT_URL + "/rest/v1/comic_categories?comic_id=eq." + comicId)
                .delete()
                .addHeader("Authorization", "Bearer " + sessionManager.getAccessToken())
                .addHeader("apikey", SupabaseConfig.API_KEY)
                .build();

        client.newCall(deleteRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    btnSave.setEnabled(true);
                    Toast.makeText(EditInformationActivity.this, "Cập nhật thông tin truyện thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (bodyArray.length() == 0) {
                    runOnUiThread(() -> {
                        btnSave.setEnabled(true);
                        Toast.makeText(EditInformationActivity.this, "Cập nhật thông tin truyện thành công!", Toast.LENGTH_SHORT).show();
                        finish();
                    });
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
                        runOnUiThread(() -> {
                            btnSave.setEnabled(true);
                            finish();
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response) {
                        runOnUiThread(() -> {
                            btnSave.setEnabled(true);
                            Toast.makeText(EditInformationActivity.this, "Cập nhật thông tin truyện thành công!", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    }
                });
            }
        });
    }

    private void showDeleteConfirmation() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Xóa truyện")
                .setMessage("Bạn có chắc chắn muốn xóa vĩnh viễn bộ truyện này không?")
                .setPositiveButton("Xóa vĩnh viễn", (dialog, which) -> executeDeleteComic())
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void executeDeleteComic() {
        String accessToken = sessionManager.getAccessToken();
        Request request = new Request.Builder()
                .url(SupabaseConfig.PROJECT_URL + "/rest/v1/comics?id=eq." + comicId)
                .delete()
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("apikey", SupabaseConfig.API_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}

            @Override
            public void onResponse(Call call, Response response) {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(EditInformationActivity.this, "Đã xóa truyện thành công", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            }
        });
    }
}