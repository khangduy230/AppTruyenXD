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
    private final int comicId;
    private final String comicName;

    public ChapterManageAdapter(List<Chapter> chapterList, int comicId, String comicName) {
        this.chapterList = chapterList;
        this.comicId = comicId;
        this.comicName = comicName;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chapter_manage, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Chapter chapter = chapterList.get(position);

        holder.tvChapterName.setText(chapter.getChapterName());
        holder.tvChapterDate.setText(DateTimeUtils.formatChapterDate(chapter.getUpdatedAt()));
        holder.tvChapterStatus.setVisibility(chapter.isHidden() ? View.VISIBLE : View.GONE);
        holder.itemView.setAlpha(chapter.isHidden() ? 0.65f : 1f);

        View.OnClickListener openEditListener = v -> {
            Context context = v.getContext();
            Intent intent = new Intent(context, EditChapterActivity.class);
            intent.putExtra("CHAPTER_ID", chapter.getId());
            intent.putExtra("COMIC_ID", comicId);
            intent.putExtra("COMIC_NAME", comicName);
            context.startActivity(intent);
        };

        holder.layoutChapterContent.setOnClickListener(openEditListener);
        holder.btnEditChapter.setOnClickListener(openEditListener);
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
        TextView tvChapterName, tvChapterDate, tvChapterStatus;
        View layoutChapterContent, btnEditChapter;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutChapterContent = itemView.findViewById(R.id.layoutChapterContent);
            tvChapterName = itemView.findViewById(R.id.tvChapterName);
            tvChapterDate = itemView.findViewById(R.id.tvChapterDate);
            tvChapterStatus = itemView.findViewById(R.id.tvChapterStatus);
            btnEditChapter = itemView.findViewById(R.id.btnEditChapter);
        }
    }
}
