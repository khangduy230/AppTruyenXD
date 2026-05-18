package com.nhom5.ftcomic.models;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "binh_luan",
        foreignKeys = {
                // Khóa ngoại 1: Liên kết (Bảng TaiKhoan)
                @ForeignKey(entity = TaiKhoan.class,
                        parentColumns = "id",
                        childColumns = "ma_tk",
                        onDelete = ForeignKey.CASCADE),

                // Khóa ngoại 2: Liên kết (Bảng Truyen)
                @ForeignKey(entity = Truyen.class,
                        parentColumns = "id",
                        childColumns = "ma_truyen",
                        onDelete = ForeignKey.CASCADE)
        },

        indices = {@Index("ma_tk"), @Index("ma_truyen")})
public class BinhLuan {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "ma_tk")
    public int maTk;

    @ColumnInfo(name = "ma_truyen")
    public int maTruyen;

    @ColumnInfo(name = "noi_dung")
    public String noiDung;

    @ColumnInfo(name = "thoi_gian")
    public String thoiGian;

    public BinhLuan(int maTk, int maTruyen, String noiDung, String thoiGian) {
        this.maTk = maTk;
        this.maTruyen = maTruyen;
        this.noiDung = noiDung;
        this.thoiGian = thoiGian;
    }
}