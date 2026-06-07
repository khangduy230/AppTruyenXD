package com.nhom5.ftcomic.adapters;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.models.Comment;
import com.nhom5.ftcomic.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentViewHolder> {

    private List<Comment> commentList = new ArrayList<>();
    private final OnReplyClickListener replyClickListener;

    public interface OnReplyClickListener {
        void onReplyClick(Comment parentComment);
    }

    public CommentsAdapter(OnReplyClickListener replyClickListener) {
        this.replyClickListener = replyClickListener;
    }

    public void setCommentList(List<Comment> comments) {
        List<Comment> sortedList = new ArrayList<>();
        for (Comment c : comments) {
            if (c.getParentId() == 0) {
                sortedList.add(c);
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
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentList.get(position);
        Context context = holder.itemView.getContext();

        // Tên người dùng
        holder.tvUser.setText(comment.getUserName());
        holder.tvContent.setText(comment.getContent());

        // Thời gian
        CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                comment.getCreatedAt(),
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS);
        holder.tvTime.setText(timeAgo);

        // ✅ Load avatar
        String avatarUri = comment.getAvatarUri();
        if (avatarUri != null && !avatarUri.isEmpty()) {
            if (avatarUri.startsWith("http")) {
                Glide.with(context)
                        .load(avatarUri)
                        .circleCrop()
                        .placeholder(R.drawable.ic_profile)
                        .into(holder.ivAvatar);
            } else {
                holder.ivAvatar.setImageURI(android.net.Uri.parse(avatarUri));
            }
            holder.ivAvatar.setImageTintList(null);
        } else {
            // Không có avatar thì dùng icon mặc định
            holder.ivAvatar.setColorFilter(
                    context.getResources().getColor(android.R.color.darker_gray, context.getTheme())
            );
            holder.ivAvatar.setImageTintList(null);
        }

        ViewGroup.MarginLayoutParams params =
                (ViewGroup.MarginLayoutParams) holder.layoutItemBody.getLayoutParams();
        int margin16 = (int) (16 * context.getResources().getDisplayMetrics().density);
        int margin48 = (int) (48 * context.getResources().getDisplayMetrics().density);

        if (comment.getParentId() > 0) {
            params.setMargins(margin48, 0, 0, margin16);
            holder.itemView.setBackgroundColor(0xFFF9F9F9);
        } else {
            params.setMargins(0, 0, 0, margin16);
            holder.itemView.setBackgroundColor(0x00000000);
        }
        holder.layoutItemBody.setLayoutParams(params);
        holder.tvReplyAction.setVisibility(View.VISIBLE);

        // ✅ Kiểm tra đăng nhập trước khi cho reply
        holder.tvReplyAction.setOnClickListener(v -> {
            SessionManager sessionManager = new SessionManager(context);
            if (!sessionManager.isLoggedIn()) {
                Toast.makeText(context,
                        "Vui lòng đăng nhập để trả lời!", Toast.LENGTH_SHORT).show();
                return;
            }
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
        ShapeableImageView ivAvatar;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUser = itemView.findViewById(R.id.tvUserName);
            tvContent = itemView.findViewById(R.id.tvCommentContent);
            tvTime = itemView.findViewById(R.id.tvCommentTime);
            tvReplyAction = itemView.findViewById(R.id.tvReplyAction);
            layoutItemBody = itemView.findViewById(R.id.layoutItemBody);
            ivAvatar = itemView.findViewById(R.id.imgAvatar); // ✅
        }
    }
}