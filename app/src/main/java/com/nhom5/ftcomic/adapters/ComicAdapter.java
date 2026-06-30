package com.nhom5.ftcomic.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.activities.DetailComicActivity;
import com.nhom5.ftcomic.models.Comic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComicAdapter extends RecyclerView.Adapter<ComicAdapter.ComicViewHolder> {

    private List<Comic> comicList;
    private OnComicClickListener listener;

    private Map<Integer, String> historyChapterMap = new HashMap<>();

    public interface OnComicClickListener {
        void onComicClick(Comic comic);
    }

    public ComicAdapter(List<Comic> comicList, OnComicClickListener listener) {
        this.comicList = comicList;
        this.listener = listener;
    }

    // Setter để Activity truyền dữ liệu chương đọc dở vào
    public void setHistoryChapterMap(Map<Integer, String> map) {
        if (map != null) {
            this.historyChapterMap = map;
        }
    }

    @NonNull
    @Override
    public ComicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comic, parent, false);

        if (parent instanceof RecyclerView) {
            RecyclerView.LayoutManager lm = ((RecyclerView) parent).getLayoutManager();
            if (lm instanceof LinearLayoutManager &&
                    ((LinearLayoutManager) lm).getOrientation() == LinearLayoutManager.HORIZONTAL) {

                ViewGroup.LayoutParams params = view.getLayoutParams();
                params.width = (int) (130 * parent.getContext().getResources().getDisplayMetrics().density);
                view.setLayoutParams(params);
            }
        }

        return new ComicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ComicViewHolder holder, int position) {
        Comic comic = comicList.get(position);

        if (comic == null) {
            return;
        }

        holder.tvComicTitle.setText(comic.getName());

        if (historyChapterMap != null && historyChapterMap.containsKey(comic.getId())) {
            holder.tvLatestChapter.setVisibility(View.VISIBLE);
            holder.tvLatestChapter.setText(historyChapterMap.get(comic.getId()));
        } else {
            holder.tvLatestChapter.setVisibility(View.GONE);
        }

        if (comic.getCoverUrl() != null && !comic.getCoverUrl().isEmpty()) {
            com.bumptech.glide.Glide.with(holder.itemView.getContext())
                    .load(comic.getCoverUrl())
                    .placeholder(comic.getImage())
                    .error(comic.getImage())
                    .into(holder.imgComic);
        } else {
            holder.imgComic.setImageResource(comic.getImage());
        }

        holder.itemView.setOnClickListener(v -> {
            android.content.Context mContext = v.getContext();
            Intent intent = new Intent(mContext, DetailComicActivity.class);
            String uniqueTransitionName = "cover_" + comic.getId();
            androidx.core.view.ViewCompat.setTransitionName(holder.cardComic, uniqueTransitionName);

            intent.putExtra("TRANSITION_NAME", uniqueTransitionName);
            intent.putExtra("COMIC_ID", comic.getId());
            intent.putExtra("COMIC_COVER_URL", comic.getCoverUrl());

            androidx.core.app.ActivityOptionsCompat options =
                    androidx.core.app.ActivityOptionsCompat.makeSceneTransitionAnimation(
                            (android.app.Activity) mContext,
                            holder.cardComic,
                            uniqueTransitionName
                    );

            mContext.startActivity(intent, options.toBundle());
        });
    }

    @Override
    public int getItemCount() {
        if (comicList == null) {
            return 0;
        }
        return comicList.size();
    }

    public void setComicList(List<Comic> newComicList) {
        if (this.comicList == null) {
            this.comicList = newComicList;
            notifyItemRangeInserted(0, newComicList.size());
            return;
        }

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return comicList.size();
            }

            @Override
            public int getNewListSize() {
                return newComicList != null ? newComicList.size() : 0;
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                // Kiểm tra xem 2 item có phải là 1 (dựa vào ID)
                return comicList.get(oldItemPosition).getId() == newComicList.get(newItemPosition).getId();
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                // Kiểm tra xem nội dung (Tên, Ảnh cover) có bị thay đổi không
                Comic oldComic = comicList.get(oldItemPosition);
                Comic newComic = newComicList.get(newItemPosition);

                String oldName = oldComic.getName() != null ? oldComic.getName() : "";
                String newName = newComic.getName() != null ? newComic.getName() : "";

                String oldCover = oldComic.getCoverUrl() != null ? oldComic.getCoverUrl() : "";
                String newCover = newComic.getCoverUrl() != null ? newComic.getCoverUrl() : "";

                return oldName.equals(newName) && oldCover.equals(newCover);
            }
        });

        this.comicList = newComicList;
        diffResult.dispatchUpdatesTo(this); // Lệnh này sẽ tự động update UI mượt mà
    }

    public static class ComicViewHolder extends RecyclerView.ViewHolder {
        ImageView imgComic;
        TextView tvComicTitle, tvLatestChapter;
        MaterialCardView cardComic;

        public ComicViewHolder(@NonNull View itemView) {
            super(itemView);
            imgComic = itemView.findViewById(R.id.imgComic);
            cardComic = itemView.findViewById(R.id.cardComic);
            tvComicTitle = itemView.findViewById(R.id.tvComicTitle);
            tvLatestChapter = itemView.findViewById(R.id.tvLatestChapter);
        }
    }
}