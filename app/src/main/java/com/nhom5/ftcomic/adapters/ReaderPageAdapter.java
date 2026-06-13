package com.nhom5.ftcomic.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.models.ChapterPage;

import java.io.File;
import java.util.List;

public class ReaderPageAdapter extends RecyclerView.Adapter<ReaderPageAdapter.ReaderPageViewHolder> {

    private List<ChapterPage> pageList;

    public ReaderPageAdapter(List<ChapterPage> pageList) {
        this.pageList = pageList;
    }

    @NonNull
    @Override
    public ReaderPageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reader_page, parent, false);

        return new ReaderPageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReaderPageViewHolder holder, int position) {
        ChapterPage page = pageList.get(position);

        if (page == null) {
            holder.imgPage.setImageResource(R.drawable.thientai);
            return;
        }

        String localFilePath = page.getLocalFilePath();

        if (localFilePath != null && !localFilePath.trim().isEmpty()) {
            File localFile = new File(localFilePath);

            if (localFile.exists() && localFile.length() > 0) {
                Glide.with(holder.itemView.getContext())
                        .load(localFile)
                        .placeholder(R.drawable.thientai)
                        .error(R.drawable.thientai)
                        .fitCenter()
                        .into(holder.imgPage);

                return;
            }
        }

        String imageUrl = page.getImageUrl();

        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.thientai)
                    .error(R.drawable.thientai)
                    .fitCenter()
                    .into(holder.imgPage);
        } else {
            holder.imgPage.setImageResource(R.drawable.thientai);
        }
    }

    @Override
    public int getItemCount() {
        return pageList == null ? 0 : pageList.size();
    }

    public void setPageList(List<ChapterPage> pageList) {
        this.pageList = pageList;
        notifyDataSetChanged();
    }

    public static class ReaderPageViewHolder extends RecyclerView.ViewHolder {

        ImageView imgPage;

        public ReaderPageViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPage = itemView.findViewById(R.id.imgPage);
        }
    }
}