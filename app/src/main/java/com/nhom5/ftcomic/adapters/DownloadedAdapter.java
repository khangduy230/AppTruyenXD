package com.nhom5.ftcomic.adapters;

import android.content.Context;
import android.os.Environment;
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

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Adapter quản lý việc hiển thị danh sách các truyện đã tải xuống máy.
 * Hỗ trợ chế độ chọn nhiều để xoá và tính toán dung lượng thực tế của từng truyện.
 */
public class DownloadedAdapter extends RecyclerView.Adapter<DownloadedAdapter.ViewHolder> {

    private List<Comic> comicList; // Danh sách truyện
    private OnComicClickListener listener; // Listener xử lý sự kiện click
    private Set<Integer> selectedIds = new HashSet<>(); // Lưu trữ các ID truyện đang được chọn để xoá
    private boolean isSelectionMode = false; // Trạng thái: đang chọn để xoá hay đang xem bình thường

    // Interface để Activity bên ngoài lắng nghe các tương tác
    public interface OnComicClickListener {
        void onComicClick(Comic comic); // Khi click xem chi tiết truyện
        void onSelectionChanged(int count); // Khi số lượng truyện chọn để xoá thay đổi
    }

    public DownloadedAdapter(List<Comic> comicList, OnComicClickListener listener) {
        this.comicList = comicList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Nạp layout cho từng item truyện (item_download.xml)
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_download, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Comic comic = comicList.get(position);
        if (comic == null) return;

        Context context = holder.itemView.getContext();

        // Hiển thị tên truyện và tác giả
        holder.tvTitle.setText(comic.getName());
        holder.tvAuthor.setText(comic.getAuthor());

        // LOGIC HIỂN THỊ DUNG LƯỢNG:
        // Tính toán kích thước thư mục chứa các chương truyện đã tải của truyện này
        long sizeBytes = calculateComicTotalSize(context, comic.getId());
        holder.tvSize.setText(formatFileSize(sizeBytes));

        // Tải ảnh bìa truyện bằng Glide
        Glide.with(context)
                .load(comic.getCoverUrl())
                .placeholder(R.drawable.ic_launcher_background)
                .into(holder.imgCover);

        // Xử lý hiển thị CheckBox dựa trên chế độ chọn (isSelectionMode)
        holder.checkBox.setVisibility(isSelectionMode ? View.VISIBLE : View.GONE);
        holder.checkBox.setChecked(selectedIds.contains(comic.getId()));

        // Xử lý sự kiện click vào toàn bộ item
        holder.itemView.setOnClickListener(v -> {
            if (isSelectionMode) {
                // Nếu đang ở chế độ chọn để xoá -> Click để chọn/bỏ chọn
                toggleSelection(comic.getId());
            } else if (listener != null) {
                // Nếu đang ở chế độ thường -> Mở màn hình chi tiết truyện
                listener.onComicClick(comic);
            }
        });

        // Click trực tiếp vào CheckBox cũng thực hiện chọn/bỏ chọn
        holder.checkBox.setOnClickListener(v -> toggleSelection(comic.getId()));
    }

    /**
     * Tính tổng dung lượng của một bộ truyện từ tất cả các thư mục lưu trữ có thể có.
     * Thường là các ảnh của từng chương được lưu trong folder "downloads/comic_{id}"
     */
    private long calculateComicTotalSize(Context context, int comicId) {
        long total = 0;
        String comicFolderName = "comic_" + comicId;

        // 1. Kiểm tra trong bộ nhớ trong của ứng dụng (Thường dùng)
        total += getFolderSize(new File(context.getFilesDir(), "downloads/" + comicFolderName));

        // 2. Kiểm tra trong bộ nhớ ngoài thuộc quyền quản lý của App (Nếu máy có thẻ nhớ)
        total += getFolderSize(new File(context.getExternalFilesDir(null), "downloads/" + comicFolderName));

        // 3. Kiểm tra trong thư mục Pictures (Trường hợp lưu ảnh theo kiểu khác)
        total += getFolderSize(new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), String.valueOf(comicId)));

        return total;
    }

    /**
     * Hàm đệ quy để tính tổng kích thước (Bytes) của tất cả file trong một thư mục
     */
    private long getFolderSize(File folder) {
        long length = 0;
        if (folder == null || !folder.exists()) return 0;
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) length += file.length();
                else length += getFolderSize(file); // Nếu là thư mục con, tiếp tục cộng dồn
            }
        }
        return length;
    }

    /**
     * Chuyển đổi Bytes sang định dạng MB để người dùng dễ đọc
     */
    private String formatFileSize(long bytes) {
        if (bytes <= 0) return "0.0 MB";
        // Tính toán MB bằng cách chia cho (1024 * 1024)
        double mb = bytes / (1024.0 * 1024.0);
        return String.format(Locale.getDefault(), "%.1f MB", mb);
    }

    /**
     * Logic đảo ngược trạng thái chọn của một truyện
     */
    private void toggleSelection(int comicId) {
        if (selectedIds.contains(comicId)) {
            selectedIds.remove(comicId);
        } else {
            selectedIds.add(comicId);
        }
        notifyDataSetChanged(); // Cập nhật lại giao diện để hiển thị trạng thái CheckBox mới
        if (listener != null) {
            listener.onSelectionChanged(selectedIds.size()); // Báo cho Activity biết số lượng đã chọn
        }
    }

    /**
     * Bật/Tắt chế độ chọn nhiều truyện
     */
    public void setSelectionMode(boolean selectionMode) {
        isSelectionMode = selectionMode;
        if (!selectionMode) {
            selectedIds.clear(); // Xoá hết lựa chọn khi thoát chế độ này
        }
        notifyDataSetChanged();
    }

    /**
     * Cập nhật danh sách truyện khi dữ liệu từ Database thay đổi
     */
    public void setComicList(List<Comic> comicList) {
        this.comicList = comicList;
        notifyDataSetChanged();
    }

    /**
     * Trả về danh sách ID của các truyện đang được chọn để Activity thực hiện xoá trong DB
     */
    public Set<Integer> getSelectedIds() {
        return selectedIds;
    }

    @Override
    public int getItemCount() {
        return comicList != null ? comicList.size() : 0;
    }

    /**
     * ViewHolder giữ các tham chiếu đến các View của item truyện để tăng hiệu năng
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCover;
        TextView tvTitle, tvAuthor, tvSize;
        CheckBox checkBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCover = itemView.findViewById(R.id.imgComic2);
            tvTitle = itemView.findViewById(R.id.tvComic);
            tvAuthor = itemView.findViewById(R.id.textView3);
            tvSize = itemView.findViewById(R.id.tvdungluong);
            checkBox = itemView.findViewById(R.id.checkBox);
        }
    }
}