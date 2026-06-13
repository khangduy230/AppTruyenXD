package com.nhom5.ftcomic.activities;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.adapters.CommentsAdapter;
import com.nhom5.ftcomic.database.AppDatabase;
import com.nhom5.ftcomic.fragments.LoginFragment;
import com.nhom5.ftcomic.models.Comment;
import com.nhom5.ftcomic.utils.SessionManager;

public class CommentsActivity extends AppCompatActivity implements CommentsAdapter.OnReplyClickListener {

    private CommentsAdapter commentsAdapter;
    private AppDatabase appDatabase;
    private SessionManager sessionManager;
    private EditText edtComment;
    private LinearLayout layoutReplyHeader;
    private TextView tvReplyStatus;
    private View btnSend;

    private int comicId = -1;
    private int selectedParentCommentId = 0;
    private String replyingToUser = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment);

        comicId = getIntent().getIntExtra("COMIC_ID", -1);
        if (comicId == -1) {
            Toast.makeText(this, "Không tìm thấy dữ liệu truyện để bình luận!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        appDatabase = AppDatabase.getInstance(this);
        sessionManager = new SessionManager(this); //  khởi tạo SessionManager

        findViewById(R.id.topAppBar).setOnClickListener(v -> finish());
        RecyclerView recyclerView = findViewById(R.id.recyclerViewComments);
        edtComment = findViewById(R.id.edtComment);
        layoutReplyHeader = findViewById(R.id.layoutReplyHeader);
        tvReplyStatus = findViewById(R.id.tvReplyStatus);
        btnSend = findViewById(R.id.btnSend);
        TextView tvCancelReply = findViewById(R.id.tvCancelReply);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        commentsAdapter = new CommentsAdapter(this);
        recyclerView.setAdapter(commentsAdapter);

        appDatabase.commentDao().getCommentsByComicId(comicId).observe(this, comments -> {
            if (comments != null) {
                commentsAdapter.setCommentList(comments);
            }
        });

        tvCancelReply.setOnClickListener(v -> exitReplyMode());

        // Kiểm tra đăng nhập khi bấm gửi
        btnSend.setOnClickListener(v -> {
            if (!sessionManager.isLoggedIn()) {
                // Chưa đăng nhập → mở LoginFragment
                LoginFragment loginFragment = new LoginFragment();
                loginFragment.show(getSupportFragmentManager(), "LoginFragment");

                // Lắng nghe sau khi đăng nhập thành công
                getSupportFragmentManager().setFragmentResultListener(
                        "key_dang_nhap",
                        this,
                        (requestKey, result) -> sendNewComment()
                );
                return;
            }
            sendNewComment();
        });

        //  Kiểm tra đăng nhập khi bấm vào ô nhập
        edtComment.setOnClickListener(v -> {
            if (!sessionManager.isLoggedIn()) {
                LoginFragment loginFragment = new LoginFragment();
                loginFragment.show(getSupportFragmentManager(), "LoginFragment");

                getSupportFragmentManager().setFragmentResultListener(
                        "key_dang_nhap",
                        this,
                        (requestKey, result) -> {
                            edtComment.requestFocus();
                            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            if (imm != null) {
                                imm.showSoftInput(edtComment, InputMethodManager.SHOW_IMPLICIT);
                            }
                        }
                );
            }
        });
    }

    @Override
    public void onReplyClick(Comment parentComment) {
        //  Kiểm tra đăng nhập khi bấm trả lời
        if (!sessionManager.isLoggedIn()) {
            LoginFragment loginFragment = new LoginFragment();
            loginFragment.show(getSupportFragmentManager(), "LoginFragment");

            getSupportFragmentManager().setFragmentResultListener(
                    "key_dang_nhap",
                    this,
                    (requestKey, result) -> setupReplyMode(parentComment)
            );
            return;
        }
        setupReplyMode(parentComment);
    }

    private void setupReplyMode(Comment parentComment) {
        replyingToUser = parentComment.getUserName();

        if (parentComment.getParentId() > 0) {
            selectedParentCommentId = parentComment.getParentId();
        } else {
            selectedParentCommentId = parentComment.getId();
        }

        layoutReplyHeader.setVisibility(View.VISIBLE);
        tvReplyStatus.setText("Đang phản hồi bình luận của " + parentComment.getUserName());

        edtComment.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(edtComment, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void sendNewComment() {
        String content = edtComment.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "Nội dung bình luận không được bỏ trống!", Toast.LENGTH_SHORT).show();
            return;
        }

        String finalContent = content;
        if (selectedParentCommentId > 0 && !replyingToUser.isEmpty()) {
            finalContent = "@" + replyingToUser + ": " + content;
        }

        //  Lấy tên và avatar thật từ SessionManager
        String savedUsername = sessionManager.getUsername();
        String email = sessionManager.getEmail();
        String displayName;

        if (savedUsername != null && !savedUsername.isEmpty()) {
            displayName = savedUsername;
        } else if (email != null && email.contains("@")) {
            displayName = email.substring(0, email.indexOf("@"));
        } else {
            displayName = "Người dùng ẩn danh";
        }

        String avatarUri = sessionManager.getAvatarUri(); // lấy avatar

        Comment newComment = new Comment(
                comicId,
                selectedParentCommentId,
                displayName,   //  tên thật
                avatarUri,     // avatar thật
                finalContent,
                System.currentTimeMillis()
        );

        AppDatabase.databaseWriteExecutor.execute(() -> {
            appDatabase.commentDao().insertComment(newComment);
            runOnUiThread(() -> {
                edtComment.setText("");
                hideKeyboard();
                exitReplyMode();
                Toast.makeText(CommentsActivity.this, "Gửi bình luận thành công!", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void exitReplyMode() {
        selectedParentCommentId = 0;
        replyingToUser = "";
        layoutReplyHeader.setVisibility(View.GONE);
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }
}