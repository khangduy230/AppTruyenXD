package com.example.truyentranhthanhxuan.models;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tai_khoan")
public class TaiKhoan {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "ten_dang_nhap")
    public String tenDangNhap;

    @ColumnInfo(name = "mat_khau")
    public String matKhau;

    @ColumnInfo(name = "vai_tro")
    public int vaiTro; // 1 là Admin, 0 là User thường

    public TaiKhoan(String tenDangNhap, String matKhau, int vaiTro) {
        this.tenDangNhap = tenDangNhap;
        this.matKhau = matKhau;
        this.vaiTro = vaiTro;
    }
}