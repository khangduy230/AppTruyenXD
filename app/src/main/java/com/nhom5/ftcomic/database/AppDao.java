package com.nhom5.ftcomic.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

import com.nhom5.ftcomic.models.TaiKhoan;
import com.nhom5.ftcomic.models.Truyen;
import com.nhom5.ftcomic.models.ChuongTruyen;
import com.nhom5.ftcomic.models.BinhLuan;

@Dao
public interface AppDao {
    // --- Tài Khoản ---
    @Insert
    long insertTaiKhoan(TaiKhoan taiKhoan);

    @Query("SELECT * FROM tai_khoan WHERE ten_dang_nhap = :username AND mat_khau = :password")
    TaiKhoan login(String username, String password);

    // --- Truyện ---
    @Insert
    long insertTruyen(Truyen truyen);

    @Query("SELECT * FROM truyen WHERE trang_thai = 1")
    List<Truyen> getTruyenHoatDong();

    // --- Chương Truyện ---
    @Insert
    long insertChuong(ChuongTruyen chuong);

    @Query("SELECT * FROM chuong_truyen WHERE ma_truyen = :truyenId ORDER BY so_chuong ASC")
    List<ChuongTruyen> getDanhSachChuong(int truyenId);

    // --- Bình Luận ---
    @Insert
    long insertBinhLuan(BinhLuan binhLuan);

    @Query("SELECT * FROM binh_luan WHERE ma_truyen = :truyenId ORDER BY id DESC")
    List<BinhLuan> getBinhLuanTheoTruyen(int truyenId);
}