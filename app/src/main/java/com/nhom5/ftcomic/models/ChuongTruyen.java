package com.nhom5.ftcomic.models;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

// Khai báo Khóa ngoại liên kết với bảng Truyen
@Entity(tableName = "chuong_truyen",
        foreignKeys = @ForeignKey(entity = Truyen.class,
                parentColumns = "id",
                childColumns = "ma_truyen",
                onDelete = ForeignKey.CASCADE), // Xóa truyện thì tự động xóa hết chương
        indices = {@Index("ma_truyen")}) // Đánh index để truy vấn nhanh hơn
public class ChuongTruyen {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "ma_truyen")
    public int maTruyen; // Khóa ngoại

    @ColumnInfo(name = "so_chuong")
    public float soChuong;

    @ColumnInfo(name = "ten_chuong")
    public String tenChuong;

    @ColumnInfo(name = "noi_dung_chuong")
    public String noiDungChuong; // Lưu nội dung chữ hoặc link ảnh của chương đó

    public ChuongTruyen(int maTruyen, float soChuong, String tenChuong, String noiDungChuong) {
        this.maTruyen = maTruyen;
        this.soChuong = soChuong;
        this.tenChuong = tenChuong;
        this.noiDungChuong = noiDungChuong;
    }
}