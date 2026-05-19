package com.nhom5.ftcomic.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.models.ChapterPage;

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
        holder.imgPage.setImageResource(page.getImage());
    }

    @Override
    public int getItemCount() {
        if (pageList == null) {
            return 0;
        }
        return pageList.size();
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