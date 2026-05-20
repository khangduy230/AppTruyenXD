package com.nhom5.ftcomic.models;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "truyen")
public class Truyen {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "ten_truyen")
    public String tenTruyen;

    @ColumnInfo(name = "mo_ta")
    public String moTa;

    @ColumnInfo(name = "trang_thai")
    public int trangThai; // 1: Hoạt động, 0: Chờ duyệt (Ẩn)

    public Truyen(String tenTruyen, String moTa, int trangThai) {
        this.tenTruyen = tenTruyen;
        this.moTa = moTa;
        this.trangThai = trangThai;
    }
}