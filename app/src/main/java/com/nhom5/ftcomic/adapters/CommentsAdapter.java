package com.nhom5.ftcomic.adapters;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.models.Comment;
import java.util.ArrayList;
import java.util.List;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentViewHolder> {

    private List<Comment> commentList = new ArrayList<>();
    private final OnReplyClickListener replyClickListener;

    // Giao tiếp click nút trả lời ra ngoài Activity
    public interface OnReplyClickListener {
        void onReplyClick(Comment parentComment);
    }

    public CommentsAdapter(OnReplyClickListener replyClickListener) {
        this.replyClickListener = replyClickListener;
    }

    public void setCommentList(List<Comment> comments) {
        // Thuật toán sắp xếp hiển thị: Đưa các bình luận phản hồi nằm ngay dưới bình luận cha tương ứng
        List<Comment> sortedList = new ArrayList<>();
        for (Comment c : comments) {
            if (c.getParentId() == 0) { // Lấy các bình luận gốc trước
                sortedList.add(c);
                // Tìm kiếm xem có phản hồi nào thuộc về bình luận cha này không
                for (Comment reply : comments) {
                    if (reply.getParentId() == c.getId()) {
                        sortedList.add(reply);
                    }
                }
            }
        }
        this.commentList = sortedList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentList.get(position);

        holder.tvUser.setText(comment.getUserName());
        holder.tvContent.setText(comment.getContent());

        // Định dạng khoảng thời gian dạng "5 phút trước", "1 ngày trước"
        CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                comment.getCreatedAt(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
        holder.tvTime.setText(timeAgo);

        // --- XỬ LÝ GIAO DIỆN PHÂN CẤP VÀ KHOẢNG CÁCH ITEM ---
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) holder.layoutItemBody.getLayoutParams();

        int margin16Px = (int) (16 * holder.itemView.getContext().getResources().getDisplayMetrics().density);
        int margin48Px = (int) (48 * holder.itemView.getContext().getResources().getDisplayMetrics().density);

        if (comment.getParentId() > 0) {
            // Nếu là phản hồi (Reply): Thụt lề trái 48dp
            params.setMargins(margin48Px, 0, 0, margin16Px);

            // Cho phép hiển thị nút "Trả lời" ở cả các câu reply con
            holder.tvReplyAction.setVisibility(View.VISIBLE);

            holder.itemView.setBackgroundColor(0xFFF9F9F9); // Nền xám nhẹ cho dòng reply
        } else {
            // Nếu là bình luận gốc: Lề trái bằng 0 thông thường
            params.setMargins(0, 0, 0, margin16Px);
            holder.tvReplyAction.setVisibility(View.VISIBLE);
            holder.itemView.setBackgroundColor(0x00000000); // Trong suốt
        }
        holder.layoutItemBody.setLayoutParams(params);

        // Sự kiện khi bấm nút Trả lời
        holder.tvReplyAction.setOnClickListener(v -> {
            if (replyClickListener != null) {
                replyClickListener.onReplyClick(comment);
            }
        });
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView tvUser, tvContent, tvTime, tvReplyAction;
        LinearLayout layoutItemBody;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUser = itemView.findViewById(R.id.tvUserName);
            tvContent = itemView.findViewById(R.id.tvCommentContent);
            tvTime = itemView.findViewById(R.id.tvCommentTime);
            tvReplyAction = itemView.findViewById(R.id.tvReplyAction);
            layoutItemBody = itemView.findViewById(R.id.layoutItemBody);
        }
    }
}