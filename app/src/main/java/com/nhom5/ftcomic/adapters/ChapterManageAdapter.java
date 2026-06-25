package com.nhom5.ftcomic.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.activities.EditChapterActivity;
import com.nhom5.ftcomic.models.Chapter;
import com.nhom5.ftcomic.utils.DateTimeUtils;

import java.util.List;

public class ChapterManageAdapter extends RecyclerView.Adapter<ChapterManageAdapter.ViewHolder> {

    private List<Chapter> chapterList;

    public ChapterManageAdapter(List<Chapter> chapterList) {
        this.chapterList = chapterList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chapter_manage, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Chapter chapter = chapterList.get(position);
        holder.tvChapterName.setText(chapter.getChapterName());
        holder.tvChapterDate.setText(DateTimeUtils.formatChapterDate(chapter.getUpdatedAt()));

        // Bấm vào nút Sửa chương (ic_pencil)
        holder.btnEditChapter.setOnClickListener(v -> {
            Context context = v.getContext();
            Intent intent = new Intent(context, EditChapterActivity.class);
            intent.putExtra("CHAPTER_ID", chapter.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return chapterList != null ? chapterList.size() : 0;
    }

    public void setChapterList(List<Chapter> chapterList) {
        this.chapterList = chapterList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvChapterName, tvChapterDate;
        View btnEditChapter;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvChapterName = itemView.findViewById(R.id.tvChapterName);
            tvChapterDate = itemView.findViewById(R.id.tvChapterDate);
            btnEditChapter = itemView.findViewById(R.id.btnEditChapter);
        }
    }
}