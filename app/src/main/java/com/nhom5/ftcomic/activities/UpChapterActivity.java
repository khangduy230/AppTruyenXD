package com.nhom5.ftcomic.activities;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
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
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.network.SupabaseConfig;
import com.nhom5.ftcomic.utils.SessionManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UpChapterActivity extends AppCompatActivity {

    private static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json");
    private static final MediaType DEFAULT_MEDIA_TYPE_IMAGE = MediaType.parse("image/jpeg");

    private TextInputEditText edtChapterNumber, edtChapterName;
    private MaterialSwitch switchHideChapter;
    private TextView tvPagesSummary;
    private ExtendedFloatingActionButton btnUpload;

    private final OkHttpClient client = new OkHttpClient();
    private final List<Uri> selectedPageUris = new ArrayList<>();
    private SessionManager sessionManager;

    private int comicId = -1;
    private String comicName = "";

    private final ActivityResultLauncher<String> pickPagesLauncher = registerForActivityResult(
            new ActivityResultContracts.GetMultipleContents(),
            uris -> {
                selectedPageUris.clear();
                if (uris != null) {
                    selectedPageUris.addAll(uris);
                }
                updatePagesSummary();
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_up_chapter);

        sessionManager = new SessionManager(this);
        readIntentData();

        if (!canManageChapters()) {
            Toast.makeText(this, "Bạn không có quyền đăng chương", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        initViews();
        setupToolbar();
        setupListeners();
        updatePagesSummary();
    }

    private void readIntentData() {
        comicId = getIntent().getIntExtra("COMIC_ID", -1);
        comicName = getIntent().getStringExtra("COMIC_NAME");
        if (comicName == null) {
            comicName = "";
        }
    }

    private boolean canManageChapters() {
        if (comicId <= 0 || sessionManager == null || !sessionManager.isLoggedIn()) {
            return false;
        }
        String role = sessionManager.getRole();
        return "admin".equals(role) || "translator".equals(role);
    }

    private void initViews() {
        edtChapterNumber = findViewById(R.id.edtChapterNumber);
        edtChapterName = findViewById(R.id.edtChapterName);
        switchHideChapter = findViewById(R.id.switchHideChapter);
        tvPagesSummary = findViewById(R.id.tvPagesSummary);
        btnUpload = findViewById(R.id.btnUpload);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
            if (!comicName.trim().isEmpty()) {
                toolbar.setSubtitle(comicName);
            }
        }
    }

    private void setupListeners() {
        View btnAddPage = findViewById(R.id.btnAddPage);
        if (btnAddPage != null) {
            btnAddPage.setOnClickListener(v -> pickPagesLauncher.launch("image/*"));
        }

        btnUpload.setOnClickListener(v -> createChapter());
    }

    private void createChapter() {
        int chapterNumber = parseChapterNumber();
        String chapterName = getText(edtChapterName);

        if (chapterNumber <= 0) {
            edtChapterNumber.setError("Số chương phải lớn hơn 0");
            return;
        }

        if (chapterName.isEmpty()) {
            edtChapterName.setError("Tên chương không được để trống");
            return;
        }

        setLoading(true);

        JSONObject body = new JSONObject();
        try {
            body.put("comic_id", comicId);
            body.put("chapter_number", chapterNumber);
            body.put("chapter_name", chapterName);
            body.put("is_hidden", switchHideChapter.isChecked());
            body.put("updated_at", Instant.now().toString());
        } catch (JSONException e) {
            setLoading(false);
            return;
        }

        Request request = new Request.Builder()
                .url(SupabaseConfig.PROJECT_URL + "rest/v1/chapters")
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
                    setLoading(false);
                    Toast.makeText(UpChapterActivity.this, "Lỗi kết nối khi tạo chương", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray array = new JSONArray(response.body().string());
                        int chapterId = array.getJSONObject(0).getInt("id");
                        uploadPagesThenInsert(chapterId);
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            setLoading(false);
                            Toast.makeText(UpChapterActivity.this, "Không đọc được dữ liệu chương vừa tạo", Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        setLoading(false);
                        Toast.makeText(UpChapterActivity.this, buildChapterSaveError(response.code()), Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    private void uploadPagesThenInsert(int chapterId) {
        if (selectedPageUris.isEmpty()) {
            completeCreation();
            return;
        }

        JSONArray pageRows = new JSONArray();
        String uploadBatchId = String.valueOf(System.currentTimeMillis());
        uploadPageAt(chapterId, uploadBatchId, 0, pageRows);
    }

    private void uploadPageAt(int chapterId, String uploadBatchId, int index, JSONArray pageRows) {
        if (index >= selectedPageUris.size()) {
            insertChapterPages(pageRows);
            return;
        }

        int pageNumber = index + 1;
        Uri pageUri = selectedPageUris.get(index);
        byte[] imageBytes;
        try {
            imageBytes = readBytes(pageUri);
        } catch (IOException e) {
            runOnUiThread(() -> {
                setLoading(false);
                Toast.makeText(UpChapterActivity.this, "Không đọc được ảnh trang " + pageNumber, Toast.LENGTH_SHORT).show();
            });
            return;
        }

        String extension = getImageExtension(pageUri);
        String filePath = "chapters/comic-" + comicId
                + "/chapter-" + chapterId
                + "/" + uploadBatchId
                + "/page-" + pageNumber + "." + extension;

        Request request = new Request.Builder()
                .url(SupabaseConfig.PROJECT_URL + "storage/v1/object/"
                        + SupabaseConfig.COMICS_BUCKET + "/" + filePath)
                .post(RequestBody.create(imageBytes, getImageMediaType(pageUri)))
                .addHeader("Authorization", "Bearer " + sessionManager.getAccessToken())
                .addHeader("apikey", SupabaseConfig.API_KEY)
                .addHeader("x-upsert", "true")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(UpChapterActivity.this, "Lỗi tải ảnh trang " + pageNumber, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) {
                if (response.isSuccessful()) {
                    try {
                        JSONObject row = new JSONObject();
                        row.put("chapter_id", chapterId);
                        row.put("page_number", pageNumber);
                        row.put("image_url", SupabaseConfig.STORAGE_PUBLIC_URL
                                + SupabaseConfig.COMICS_BUCKET + "/" + filePath);
                        pageRows.put(row);
                        uploadPageAt(chapterId, uploadBatchId, index + 1, pageRows);
                    } catch (JSONException e) {
                        runOnUiThread(() -> {
                            setLoading(false);
                            Toast.makeText(UpChapterActivity.this, "Không tạo được dữ liệu trang", Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        setLoading(false);
                        Toast.makeText(UpChapterActivity.this, "Tải ảnh thất bại: " + response.code(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void insertChapterPages(JSONArray pageRows) {
        if (pageRows.length() == 0) {
            completeCreation();
            return;
        }

        Request request = new Request.Builder()
                .url(SupabaseConfig.PROJECT_URL + "rest/v1/chapter_pages")
                .post(RequestBody.create(pageRows.toString(), MEDIA_TYPE_JSON))
                .addHeader("Authorization", "Bearer " + sessionManager.getAccessToken())
                .addHeader("apikey", SupabaseConfig.API_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(UpChapterActivity.this, "Đã tạo chương nhưng chưa lưu được trang", Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) {
                if (response.isSuccessful()) {
                    completeCreation();
                } else {
                    runOnUiThread(() -> {
                        setLoading(false);
                        Toast.makeText(UpChapterActivity.this, "Lưu trang thất bại: " + response.code(), Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    private byte[] readBytes(Uri uri) throws IOException {
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            if (inputStream == null) {
                throw new IOException("Cannot open selected image");
            }
            byte[] buffer = new byte[4096];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            return outputStream.toByteArray();
        }
    }

    private MediaType getImageMediaType(Uri uri) {
        String mimeType = getContentResolver().getType(uri);
        MediaType mediaType = mimeType == null ? null : MediaType.parse(mimeType);
        return mediaType != null ? mediaType : DEFAULT_MEDIA_TYPE_IMAGE;
    }

    private String getImageExtension(Uri uri) {
        String mimeType = getContentResolver().getType(uri);
        if ("image/png".equals(mimeType)) {
            return "png";
        }
        if ("image/webp".equals(mimeType)) {
            return "webp";
        }
        if ("image/gif".equals(mimeType)) {
            return "gif";
        }
        return "jpg";
    }

    private int parseChapterNumber() {
        try {
            return Integer.parseInt(getText(edtChapterNumber));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private String getText(TextInputEditText editText) {
        if (editText == null || editText.getText() == null) {
            return "";
        }
        return editText.getText().toString().trim();
    }

    private void updatePagesSummary() {
        if (tvPagesSummary == null) {
            return;
        }
        if (selectedPageUris.isEmpty()) {
            tvPagesSummary.setText("Chưa chọn ảnh trang nào");
        } else {
            tvPagesSummary.setText("Đã chọn " + selectedPageUris.size() + " ảnh trang");
        }
    }

    private void setLoading(boolean loading) {
        btnUpload.setEnabled(!loading);
        btnUpload.setText(loading ? "Đang tải..." : "Tải lên");
    }

    private String buildChapterSaveError(int code) {
        if (code == 409) {
            return "Số chương đã tồn tại trong truyện này";
        }
        if (code == 400) {
            return "Không lưu được chương. Hãy chạy SQL thêm cột is_hidden trong Supabase trước.";
        }
        return "Tạo chương thất bại: " + code;
    }

    private void completeCreation() {
        runOnUiThread(() -> {
            setLoading(false);
            Toast.makeText(UpChapterActivity.this, "Đã tạo chương mới", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
