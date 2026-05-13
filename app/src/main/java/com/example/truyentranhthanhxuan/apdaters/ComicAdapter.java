package com.example.truyentranhthanhxuan.apdaters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.truyentranhthanhxuan.R;
import com.example.truyentranhthanhxuan.models.Comic;

import java.util.List;

public class ComicAdapter extends RecyclerView.Adapter<ComicAdapter.ComicViewHolder> {

    private List<Comic> comicList;
    private OnComicClickListener listener;

    // Interface để xử lý sự kiện click
    public interface OnComicClickListener {
        void onComicClick(Comic comic);
    }

    public ComicAdapter(List<Comic> comicList, OnComicClickListener listener) {
        this.comicList = comicList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ComicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comic, parent, false);
        return new ComicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ComicViewHolder holder, int position) {
        Comic comic = comicList.get(position);
        if (comic == null) return;

        // Gán ảnh từ drawable
        holder.imgComic.setImageResource(comic.getImage());

        // Bắt sự kiện click vào item
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onComicClick(comic);
            }
        });
    }

    @Override
    public int getItemCount() {
        if (comicList != null) {
            return comicList.size();
        }
        return 0;
    }

    public static class ComicViewHolder extends RecyclerView.ViewHolder {
        ImageView imgComic;

        public ComicViewHolder(@NonNull View itemView) {
            super(itemView);
            imgComic = itemView.findViewById(R.id.imgComic);
        }
    }
}