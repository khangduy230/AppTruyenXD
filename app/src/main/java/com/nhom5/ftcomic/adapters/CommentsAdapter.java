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
    private final OnCommentClickListener clickListener;

    // Đổi tên và tích hợp thêm sự kiện xóa bình luận
    public interface OnCommentClickListener {
        void onReplyClick(Comment parentComment);
        void onDeleteClick(Comment comment);
    }

    public CommentsAdapter(OnCommentClickListener clickListener) {
        this.clickListener = clickListener;
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
        SessionManager sessionManager = new SessionManager(context);

        holder.tvUser.setText(comment.getUserName());
        holder.tvContent.setText(comment.getContent());

        if (comment.getChapterId() > 0 && comment.getChapterName() != null && !comment.getChapterName().isEmpty()) {
            holder.tvLevel.setVisibility(View.VISIBLE);
            holder.tvLevel.setText("- " + comment.getChapterName());
        } else {
            holder.tvLevel.setVisibility(View.GONE);
        }

        CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                comment.getCreatedAt(),
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS);
        holder.tvTime.setText(timeAgo);

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

        if (sessionManager.isLoggedIn()) {
            String currentUsername = sessionManager.getUsername();
            String currentRole = sessionManager.getRole();

            boolean isAdmin = "admin".equals(currentRole);
            boolean isOwner = currentUsername != null && currentUsername.equals(comment.getUserName());

            if (isAdmin || isOwner) {
                holder.tvDeleteAction.setVisibility(View.VISIBLE);
            } else {
                holder.tvDeleteAction.setVisibility(View.GONE);
            }
        } else {
            holder.tvDeleteAction.setVisibility(View.GONE);
        }

        holder.tvReplyAction.setOnClickListener(v -> {
            if (!sessionManager.isLoggedIn()) {
                Toast.makeText(context, "Vui lòng đăng nhập để trả lời!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (clickListener != null) {
                clickListener.onReplyClick(comment);
            }
        });

        holder.tvDeleteAction.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onDeleteClick(comment);
            }
        });
    }
    @Override
    public int getItemCount() {
        return commentList.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView tvUser, tvContent, tvTime, tvReplyAction, tvLevel, tvDeleteAction;
        LinearLayout layoutItemBody;
        ShapeableImageView ivAvatar;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUser = itemView.findViewById(R.id.tvUserName);
            tvLevel = itemView.findViewById(R.id.tvLevel);
            tvContent = itemView.findViewById(R.id.tvCommentContent);
            tvTime = itemView.findViewById(R.id.tvCommentTime);
            tvReplyAction = itemView.findViewById(R.id.tvReplyAction);
            layoutItemBody = itemView.findViewById(R.id.layoutItemBody);
            ivAvatar = itemView.findViewById(R.id.imgAvatar);

            tvDeleteAction = itemView.findViewById(R.id.tvDeleteAction);
        }
    }
}