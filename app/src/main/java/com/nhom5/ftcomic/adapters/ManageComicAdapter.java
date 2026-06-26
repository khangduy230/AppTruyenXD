package com.nhom5.ftcomic.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.activities.ChapterManageActivity;
import com.nhom5.ftcomic.activities.EditInformationActivity;
import com.nhom5.ftcomic.network.response.ComicResponse;

import java.util.List;

public class ManageComicAdapter extends RecyclerView.Adapter<ManageComicAdapter.ViewHolder> {

    private final List<ComicResponse> comicList;

    public ManageComicAdapter(List<ComicResponse> comicList) {
        this.comicList = comicList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_manage_comic, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ComicResponse comic = comicList.get(position);
        holder.tvComicTitle.setText(comic.getName());

        String author = comic.getAuthor();
        holder.tvComicGenre.setText(author != null && !author.trim().isEmpty() ? author : "Tác giả: Chưa cập nhật");

        Glide.with(holder.itemView.getContext())
                .load(comic.getCoverUrl())
                .placeholder(R.drawable.ic_profile)
                .into(holder.ivComicCover);

        holder.btnEditInfo.setOnClickListener(v -> {
            Context context = v.getContext();
            Intent intent = new Intent(context, EditInformationActivity.class);
            intent.putExtra("COMIC_ID", String.valueOf(comic.getId()));
            context.startActivity(intent);
        });

        holder.btnManageChapters.setOnClickListener(v -> {
            Context context = v.getContext();
            Intent intent = new Intent(context, ChapterManageActivity.class);
            intent.putExtra("COMIC_ID", String.valueOf(comic.getId()));
            intent.putExtra("COMIC_NAME", comic.getName());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return comicList != null ? comicList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivComicCover;
        TextView tvComicTitle;
        TextView tvComicGenre;
        View btnEditInfo, btnManageChapters;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivComicCover = itemView.findViewById(R.id.ivComicCover);
            tvComicTitle = itemView.findViewById(R.id.tvComicTitle);
            tvComicGenre = itemView.findViewById(R.id.tvComicGenre);
            btnEditInfo = itemView.findViewById(R.id.btnEditInfo);
            btnManageChapters = itemView.findViewById(R.id.btnManageChapters);
        }
    }
}