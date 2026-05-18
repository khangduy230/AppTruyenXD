package com.nhom5.ftcomic.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.nhom5.ftcomic.models.TaiKhoan;
import com.nhom5.ftcomic.models.Truyen;
import com.nhom5.ftcomic.models.ChuongTruyen;
import com.nhom5.ftcomic.models.BinhLuan;

@Database(entities = {TaiKhoan.class, Truyen.class, ChuongTruyen.class, BinhLuan.class}, version = 3)
public abstract class AppDatabase extends RoomDatabase {

    public abstract AppDao appDao();

    private static AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "truyen_thanh_xuan.db")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return INSTANCE;
    }
}