package com.nhom5.ftcomic.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom5.ftcomic.R;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentViewHolder> {

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
       //API ném vào đây, chắc thế :v
    }

    @Override
    public int getItemCount() {
        // Dupe 5 cái comment nhìn cho đẹp
        return 5;
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}