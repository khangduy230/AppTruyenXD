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
import com.nhom5.ftcomic.network.SupabaseApi;
import com.nhom5.ftcomic.network.SupabaseClient;
import com.nhom5.ftcomic.network.request.CommentRequest;
import com.nhom5.ftcomic.network.response.CommentResponse;
import com.nhom5.ftcomic.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
        sessionManager = new SessionManager(this);

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

        btnSend.setOnClickListener(v -> {
            if (!sessionManager.isLoggedIn()) {
                openLoginThen(() -> sendNewCommentToSupabase());
                return;
            }

            sendNewCommentToSupabase();
        });

        edtComment.setOnClickListener(v -> {
            if (!sessionManager.isLoggedIn()) {
                openLoginThen(() -> {
                    edtComment.requestFocus();
                    showKeyboard();
                });
            }
        });

        syncCommentsFromSupabase();
    }

    @Override
    public void onReplyClick(Comment parentComment) {
        if (!sessionManager.isLoggedIn()) {
            openLoginThen(() -> setupReplyMode(parentComment));
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
        showKeyboard();
    }

    private void sendNewCommentToSupabase() {
        String content = edtComment.getText().toString().trim();

        if (content.isEmpty()) {
            Toast.makeText(this, "Nội dung bình luận không được bỏ trống!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (content.length() > 500) {
            Toast.makeText(this, "Bình luận tối đa 500 ký tự!", Toast.LENGTH_SHORT).show();
            return;
        }

        String finalContent = content;
        if (selectedParentCommentId > 0 && !replyingToUser.isEmpty()) {
            finalContent = "@" + replyingToUser + ": " + content;
        }

        String userId = sessionManager.getUserId();

        if (userId == null || userId.trim().isEmpty()) {
            Toast.makeText(this, "Không tìm thấy thông tin người dùng!", Toast.LENGTH_SHORT).show();
            return;
        }

        Integer parentId = selectedParentCommentId > 0 ? selectedParentCommentId : null;

        btnSend.setEnabled(false);

        CommentRequest request = new CommentRequest(
                userId,
                comicId,
                parentId,
                finalContent
        );

        SupabaseApi api = SupabaseClient.getApi(this);

        api.addComment("return=minimal", request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                btnSend.setEnabled(true);

                if (response.isSuccessful()) {
                    edtComment.setText("");
                    hideKeyboard();
                    exitReplyMode();

                    Toast.makeText(CommentsActivity.this, "Gửi bình luận thành công!", Toast.LENGTH_SHORT).show();

                    syncCommentsFromSupabase();
                } else {
                    Toast.makeText(
                            CommentsActivity.this,
                            "Gửi bình luận thất bại: " + response.code(),
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                btnSend.setEnabled(true);

                Toast.makeText(
                        CommentsActivity.this,
                        "Lỗi mạng khi gửi bình luận: " + t.getMessage(),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void syncCommentsFromSupabase() {
        SupabaseApi api = SupabaseClient.getApi(this);

        api.getCommentsByComicId(
                "eq." + comicId,
                "created_at.desc"
        ).enqueue(new Callback<List<CommentResponse>>() {
            @Override
            public void onResponse(Call<List<CommentResponse>> call, Response<List<CommentResponse>> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(
                            CommentsActivity.this,
                            "Không tải được bình luận: " + response.code(),
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                List<CommentResponse> remoteComments = response.body();
                if (remoteComments == null) {
                    remoteComments = new ArrayList<>();
                }

                List<Comment> localComments = mapRemoteCommentsToLocal(remoteComments);
                int remoteCommentCount = localComments.size();

                AppDatabase.databaseWriteExecutor.execute(() -> {
                    appDatabase.commentDao().deleteCommentsByComicId(comicId);
                    appDatabase.commentDao().insertComments(localComments);

                    // Cập nhật số bình luận vào bảng comics local
                    appDatabase.comicDao().updateCommentCount(comicId, remoteCommentCount);
                });
            }

            @Override
            public void onFailure(Call<List<CommentResponse>> call, Throwable t) {
                Toast.makeText(
                        CommentsActivity.this,
                        "Lỗi mạng khi tải bình luận: " + t.getMessage(),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private List<Comment> mapRemoteCommentsToLocal(List<CommentResponse> responses) {
        List<Comment> comments = new ArrayList<>();

        for (CommentResponse item : responses) {
            Comment comment = new Comment(
                    item.getComicId(),
                    item.getParentId(),
                    item.getUsername(),
                    item.getAvatarUrl(),
                    item.getContent(),
                    item.getCreatedAtMillis()
            );

            comment.setId(item.getId());
            comments.add(comment);
        }

        return comments;
    }

    private void openLoginThen(Runnable afterLogin) {
        LoginFragment loginFragment = new LoginFragment();
        loginFragment.show(getSupportFragmentManager(), "LoginFragment");

        getSupportFragmentManager().setFragmentResultListener(
                "key_dang_nhap",
                this,
                (requestKey, result) -> afterLogin.run()
        );
    }

    private void exitReplyMode() {
        selectedParentCommentId = 0;
        replyingToUser = "";
        layoutReplyHeader.setVisibility(View.GONE);
    }

    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        if (imm != null) {
            imm.showSoftInput(edtComment, InputMethodManager.SHOW_IMPLICIT);
        }
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