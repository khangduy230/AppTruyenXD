package com.nhom5.ftcomic.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.models.Comic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DownloadedAdapter extends RecyclerView.Adapter<DownloadedAdapter.ViewHolder> {

    private List<Comic> comicList;
    private OnComicClickListener listener;
    private Set<Integer> selectedIds = new HashSet<>();
    private boolean isSelectionMode = false;

    public interface OnComicClickListener {
        void onComicClick(Comic comic);
        void onSelectionChanged(int count);
    }

    public DownloadedAdapter(List<Comic> comicList, OnComicClickListener listener) {
        this.comicList = comicList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_download, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Comic comic = comicList.get(position);
        if (comic == null) return;

        holder.tvTitle.setText(comic.getName());
        holder.tvAuthor.setText(comic.getAuthor());
        holder.tvSize.setText("500 MB"); // Giả lập dung lượng

        Glide.with(holder.itemView.getContext())
                .load(comic.getCoverUrl())
                .placeholder(R.drawable.ic_launcher_background)
                .into(holder.imgCover);

        // Hiển thị/Ẩn CheckBox và trạng thái chọn
        holder.checkBox.setVisibility(isSelectionMode ? View.VISIBLE : View.GONE);
        holder.checkBox.setChecked(selectedIds.contains(comic.getId()));

        holder.itemView.setOnClickListener(v -> {
            if (isSelectionMode) {
                toggleSelection(comic.getId());
            } else if (listener != null) {
                listener.onComicClick(comic);
            }
        });

        holder.checkBox.setOnClickListener(v -> toggleSelection(comic.getId()));
    }

    private void toggleSelection(int comicId) {
        if (selectedIds.contains(comicId)) {
            selectedIds.remove(comicId);
        } else {
            selectedIds.add(comicId);
        }
        notifyDataSetChanged();
        if (listener != null) {
            listener.onSelectionChanged(selectedIds.size());
        }
    }

    public void setSelectionMode(boolean selectionMode) {
        isSelectionMode = selectionMode;
        if (!selectionMode) {
            selectedIds.clear();
        }
        notifyDataSetChanged();
    }

    public void setComicList(List<Comic> comicList) {
        this.comicList = comicList;
        notifyDataSetChanged();
    }

    public Set<Integer> getSelectedIds() {
        return selectedIds;
    }

    @Override
    public int getItemCount() {
        return comicList != null ? comicList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCover;
        TextView tvTitle, tvAuthor, tvSize;
        CheckBox checkBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCover = itemView.findViewById(R.id.imgComic2);
            tvTitle = itemView.findViewById(R.id.tvComic);
            tvAuthor = itemView.findViewById(R.id.textView3);
            tvSize = itemView.findViewById(R.id.textView5);
            checkBox = itemView.findViewById(R.id.checkBox);
        }
    }
}