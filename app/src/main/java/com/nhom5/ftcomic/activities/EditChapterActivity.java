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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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

public class EditChapterActivity extends AppCompatActivity {

    private static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json");
    private static final MediaType DEFAULT_MEDIA_TYPE_IMAGE = MediaType.parse("image/jpeg");

    private TextInputEditText edtChapterNumber, edtChapterName;
    private MaterialSwitch switchHideChapter;
    private TextView tvPagesSummary;
    private ExtendedFloatingActionButton btnSave;
    private FloatingActionButton btnDelete, btnHide;

    private final OkHttpClient client = new OkHttpClient();
    private final List<Uri> selectedPageUris = new ArrayList<>();
    private SessionManager sessionManager;

    private int chapterId = -1;
    private int comicId = -1;
    private int existingPageCount = 0;
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
        setContentView(R.layout.activity_edit_chapter);

        sessionManager = new SessionManager(this);
        readIntentData();

        if (!canManageChapters()) {
            Toast.makeText(this, "Bạn không có quyền sửa chương", Toast.LENGTH_SHORT).show();
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
        fetchChapterDetail();
        fetchPageCount();
    }

    private void readIntentData() {
        chapterId = getIntent().getIntExtra("CHAPTER_ID", -1);
        comicId = getIntent().getIntExtra("COMIC_ID", -1);
        comicName = getIntent().getStringExtra("COMIC_NAME");
        if (comicName == null) {
            comicName = "";
        }
    }

    private boolean canManageChapters() {
        if (chapterId <= 0 || sessionManager == null || !sessionManager.isLoggedIn()) {
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
        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);
        btnHide = findViewById(R.id.btnHide);
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

        btnSave.setOnClickListener(v -> updateChapter());
        btnDelete.setOnClickListener(v -> confirmDeleteChapter());

        if (btnHide != null) {
            btnHide.setOnClickListener(v -> switchHideChapter.setChecked(!switchHideChapter.isChecked()));
        }
    }

    private void fetchChapterDetail() {
        Request request = new Request.Builder()
                .url(SupabaseConfig.PROJECT_URL + "rest/v1/chapters?id=eq." + chapterId + "&select=*")
                .get()
                .addHeader("Authorization", "Bearer " + sessionManager.getAccessToken())
                .addHeader("apikey", SupabaseConfig.API_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(EditChapterActivity.this, "Lỗi tải chương", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray array = new JSONArray(response.body().string());
                        if (array.length() == 0) {
                            runOnUiThread(() -> Toast.makeText(EditChapterActivity.this, "Không tìm thấy chương", Toast.LENGTH_SHORT).show());
                            return;
                        }

                        JSONObject chapter = array.getJSONObject(0);
                        runOnUiThread(() -> populateChapter(chapter));
                    } catch (Exception e) {
                        runOnUiThread(() -> Toast.makeText(EditChapterActivity.this, "Không đọc được dữ liệu chương", Toast.LENGTH_SHORT).show());
                    }
                }
            }
        });
    }

    private void populateChapter(JSONObject chapter) {
        edtChapterNumber.setText(String.valueOf(chapter.optInt("chapter_number", 1)));
        edtChapterName.setText(chapter.optString("chapter_name", ""));
        switchHideChapter.setChecked(chapter.optBoolean("is_hidden", false));

        if (comicId <= 0) {
            comicId = chapter.optInt("comic_id", -1);
        }
    }

    private void fetchPageCount() {
        Request request = new Request.Builder()
                .url(SupabaseConfig.PROJECT_URL + "rest/v1/chapter_pages?chapter_id=eq."
                        + chapterId + "&select=id")
                .get()
                .addHeader("Authorization", "Bearer " + sessionManager.getAccessToken())
                .addHeader("apikey", SupabaseConfig.API_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> updatePagesSummary());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray array = new JSONArray(response.body().string());
                        existingPageCount = array.length();
                    } catch (Exception ignored) {
                        existingPageCount = 0;
                    }
                }
                runOnUiThread(() -> updatePagesSummary());
            }
        });
    }

    private void updateChapter() {
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
            body.put("chapter_number", chapterNumber);
            body.put("chapter_name", chapterName);
            body.put("is_hidden", switchHideChapter.isChecked());
            body.put("updated_at", Instant.now().toString());
        } catch (JSONException e) {
            setLoading(false);
            return;
        }

        Request request = new Request.Builder()
                .url(SupabaseConfig.PROJECT_URL + "rest/v1/chapters?id=eq." + chapterId)
                .patch(RequestBody.create(body.toString(), MEDIA_TYPE_JSON))
                .addHeader("Authorization", "Bearer " + sessionManager.getAccessToken())
                .addHeader("apikey", SupabaseConfig.API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(EditChapterActivity.this, "Lỗi kết nối khi lưu chương", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) {
                if (response.isSuccessful()) {
                    if (selectedPageUris.isEmpty()) {
                        completeUpdate();
                    } else {
                        uploadPagesThenReplaceRows();
                    }
                } else {
                    runOnUiThread(() -> {
                        setLoading(false);
                        Toast.makeText(EditChapterActivity.this, buildChapterSaveError(response.code()), Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    private void uploadPagesThenReplaceRows() {
        JSONArray pageRows = new JSONArray();
        String uploadBatchId = String.valueOf(System.currentTimeMillis());
        uploadPageAt(uploadBatchId, 0, pageRows);
    }

    private void deletePagesBeforeInserting(JSONArray pageRows) {
        Request request = new Request.Builder()
                .url(SupabaseConfig.PROJECT_URL + "rest/v1/chapter_pages?chapter_id=eq." + chapterId)
                .delete()
                .addHeader("Authorization", "Bearer " + sessionManager.getAccessToken())
                .addHeader("apikey", SupabaseConfig.API_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(EditChapterActivity.this, "Không xóa được trang cũ", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) {
                if (response.isSuccessful()) {
                    insertChapterPages(pageRows);
                } else {
                    runOnUiThread(() -> {
                        setLoading(false);
                        Toast.makeText(EditChapterActivity.this, "Xóa trang cũ thất bại: " + response.code(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void uploadPageAt(String uploadBatchId, int index, JSONArray pageRows) {
        if (index >= selectedPageUris.size()) {
            deletePagesBeforeInserting(pageRows);
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
                Toast.makeText(EditChapterActivity.this, "Không đọc được ảnh trang " + pageNumber, Toast.LENGTH_SHORT).show();
            });
            return;
        }

        int safeComicId = comicId > 0 ? comicId : 0;
        String extension = getImageExtension(pageUri);
        String filePath = "chapters/comic-" + safeComicId
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
                    Toast.makeText(EditChapterActivity.this, "Lỗi tải ảnh trang " + pageNumber, Toast.LENGTH_SHORT).show();
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
                        uploadPageAt(uploadBatchId, index + 1, pageRows);
                    } catch (JSONException e) {
                        runOnUiThread(() -> {
                            setLoading(false);
                            Toast.makeText(EditChapterActivity.this, "Không tạo được dữ liệu trang", Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        setLoading(false);
                        Toast.makeText(EditChapterActivity.this, "Tải ảnh thất bại: " + response.code(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void insertChapterPages(JSONArray pageRows) {
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
                    Toast.makeText(EditChapterActivity.this, "Đã lưu chương nhưng chưa thay được trang", Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) {
                if (response.isSuccessful()) {
                    completeUpdate();
                } else {
                    runOnUiThread(() -> {
                        setLoading(false);
                        Toast.makeText(EditChapterActivity.this, "Lưu trang thất bại: " + response.code(), Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    private void confirmDeleteChapter() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Xóa chương")
                .setMessage("Bạn có chắc muốn xóa vĩnh viễn chương này không?")
                .setPositiveButton("Xóa", (dialog, which) -> deleteChapterPages())
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void deleteChapterPages() {
        setLoading(true);

        Request request = new Request.Builder()
                .url(SupabaseConfig.PROJECT_URL + "rest/v1/chapter_pages?chapter_id=eq." + chapterId)
                .delete()
                .addHeader("Authorization", "Bearer " + sessionManager.getAccessToken())
                .addHeader("apikey", SupabaseConfig.API_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(EditChapterActivity.this, "Không xóa được trang của chương", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) {
                if (response.isSuccessful()) {
                    deleteChapterRow();
                } else {
                    runOnUiThread(() -> {
                        setLoading(false);
                        Toast.makeText(EditChapterActivity.this, "Xóa trang thất bại: " + response.code(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void deleteChapterRow() {
        Request request = new Request.Builder()
                .url(SupabaseConfig.PROJECT_URL + "rest/v1/chapters?id=eq." + chapterId)
                .delete()
                .addHeader("Authorization", "Bearer " + sessionManager.getAccessToken())
                .addHeader("apikey", SupabaseConfig.API_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(EditChapterActivity.this, "Không xóa được chương", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) {
                runOnUiThread(() -> {
                    setLoading(false);
                    if (response.isSuccessful()) {
                        Toast.makeText(EditChapterActivity.this, "Đã xóa chương", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(EditChapterActivity.this, "Xóa chương thất bại: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                });
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
        if (!selectedPageUris.isEmpty()) {
            tvPagesSummary.setText("Sẽ thay bằng " + selectedPageUris.size() + " ảnh trang mới");
        } else if (existingPageCount > 0) {
            tvPagesSummary.setText("Đang có " + existingPageCount + " trang. Chọn ảnh mới để thay trang.");
        } else {
            tvPagesSummary.setText("Chưa có trang nào");
        }
    }

    private void setLoading(boolean loading) {
        btnSave.setEnabled(!loading);
        btnDelete.setEnabled(!loading);
        if (btnHide != null) {
            btnHide.setEnabled(!loading);
        }
        btnSave.setText(loading ? "Đang lưu..." : "Lưu");
    }

    private String buildChapterSaveError(int code) {
        if (code == 409) {
            return "Số chương đã tồn tại trong truyện này";
        }
        if (code == 400) {
            return "Không lưu được chương. Hãy chạy SQL thêm cột is_hidden trong Supabase trước.";
        }
        return "Lưu chương thất bại: " + code;
    }

    private void completeUpdate() {
        runOnUiThread(() -> {
            setLoading(false);
            Toast.makeText(EditChapterActivity.this, "Đã cập nhật chương", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
