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
import com.nhom5.ftcomic.models.Comment;

public class CommentsActivity extends AppCompatActivity implements CommentsAdapter.OnReplyClickListener {

    private CommentsAdapter commentsAdapter;
    private AppDatabase appDatabase;
    private EditText edtComment;
    private LinearLayout layoutReplyHeader;
    private TextView tvReplyStatus;

    private int comicId = -1;
    private int selectedParentCommentId = 0; // Bằng 0 nghĩa là đang bình luận gốc công khai
    private String replyingToUser = ""; // Lưu tên người đang được phản hồi

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment);

        // Nhận dữ liệu ID bộ truyện được truyền sang từ ReaderActivity
        comicId = getIntent().getIntExtra("COMIC_ID", -1);
        if (comicId == -1) {
            Toast.makeText(this, "Không tìm thấy dữ liệu truyện để bình luận!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        appDatabase = AppDatabase.getInstance(this);

        // Ánh xạ View
        findViewById(R.id.topAppBar).setOnClickListener(v -> finish());
        RecyclerView recyclerView = findViewById(R.id.recyclerViewComments);
        edtComment = findViewById(R.id.edtComment);
        layoutReplyHeader = findViewById(R.id.layoutReplyHeader);
        tvReplyStatus = findViewById(R.id.tvReplyStatus);
        TextView tvCancelReply = findViewById(R.id.tvCancelReply);

        // Cấu hình RecyclerView và Adapter
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        commentsAdapter = new CommentsAdapter(this); // Đăng ký lắng nghe click Reply
        recyclerView.setAdapter(commentsAdapter);

        // --- LẮNG NGHE DỮ LIỆU LIVE DATA TỰ ĐỘNG CẬP NHẬT GIAO DIỆN ---
        appDatabase.commentDao().getCommentsByComicId(comicId).observe(this, comments -> {
            if (comments != null) {
                commentsAdapter.setCommentList(comments);
            }
        });

        // Xử lý nút bấm Hủy trạng thái trả lời
        tvCancelReply.setOnClickListener(v -> exitReplyMode());

        // Xử lý nút Gửi bình luận
        findViewById(R.id.btnSend).setOnClickListener(v -> sendNewComment());
    }

    // Thực thi khi người dùng bấm chữ "Trả lời" từ một dòng comment bất kỳ
    @Override
    public void onReplyClick(Comment parentComment) {
        // Ghi nhận tên người được rep để xử lý gắn tag
        replyingToUser = parentComment.getUserName();

        // Nếu đang bấm trả lời một câu reply, nó sẽ được đưa về làm con của bình luận lớn nhất
        if (parentComment.getParentId() > 0) {
            selectedParentCommentId = parentComment.getParentId();
        } else {
            selectedParentCommentId = parentComment.getId();
        }

        layoutReplyHeader.setVisibility(View.VISIBLE);
        tvReplyStatus.setText("Đang phản hồi bình luận của " + parentComment.getUserName());

        edtComment.requestFocus();

        // Bật bàn phím lên cho người dùng gõ
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

        // --- TỐI ƯU UX: Tự động chèn tag @Tên_User vào đầu nội dung nếu đang ở chế độ trả lời ---
        String finalContent = content;
        if (selectedParentCommentId > 0 && !replyingToUser.isEmpty()) {
            finalContent = "@" + replyingToUser + ": " + content;
        }

        Comment newComment = new Comment(
                comicId,
                selectedParentCommentId, // Gửi ID cha hiện tại (bằng 0 nếu là gốc, > 0 nếu là rep)
                "Người dùng ẩn danh",
                finalContent,
                System.currentTimeMillis()
        );

        AppDatabase.databaseWriteExecutor.execute(() -> {
            appDatabase.commentDao().insertComment(newComment);

            runOnUiThread(() -> {
                edtComment.setText("");

                // Ẩn bàn phím ảo đi sau khi gửi thành công giúp thoáng tầm nhìn
                hideKeyboard();

                // VỊ TRÍ QUAN TRỌNG: Khôi phục lại trạng thái bình luận gốc ngay sau khi gửi thành công
                exitReplyMode();

                Toast.makeText(CommentsActivity.this, "Gửi bình luận thành công!", Toast.LENGTH_SHORT).show();
            });
        });
    }

    // Hàm phụ trợ thoát trạng thái Trả lời, đưa về trạng thái bình luận gốc
    private void exitReplyMode() {
        selectedParentCommentId = 0;
        replyingToUser = ""; // Reset tên người được rep
        layoutReplyHeader.setVisibility(View.GONE);
    }

    // THÊM HÀM PHỤ TRỢ: Ẩn bàn phím ảo
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