package com.nhom5.ftcomic.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.models.Comic;

import java.util.List;

public class ComicAdapter extends RecyclerView.Adapter<ComicAdapter.ComicViewHolder> {

    private List<Comic> comicList;
    private OnComicClickListener listener;

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
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comic, parent, false);
        return new ComicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ComicViewHolder holder, int position) {
        Comic comic = comicList.get(position);

        if (comic == null) {
            return;
        }

        holder.imgComic.setImageResource(comic.getImage());
        holder.tvComicTitle.setText(comic.getTitle());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onComicClick(comic);
            }
        });
    }

    @Override
    public int getItemCount() {
        if (comicList == null) {
            return 0;
        }
        return comicList.size();
    }

    public void setComicList(List<Comic> comicList) {
        this.comicList = comicList;
        notifyDataSetChanged();
    }

    public static class ComicViewHolder extends RecyclerView.ViewHolder {
        ImageView imgComic;
        TextView tvComicTitle;

        public ComicViewHolder(@NonNull View itemView) {
            super(itemView);
            imgComic = itemView.findViewById(R.id.imgComic);
            tvComicTitle = itemView.findViewById(R.id.tvComicTitle);
        }
    }
}