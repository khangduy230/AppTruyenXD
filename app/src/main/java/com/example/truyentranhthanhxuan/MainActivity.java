package com.example.truyentranhthanhxuan;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.truyentranhthanhxuan.database.AppDatabase;
import com.example.truyentranhthanhxuan.database.AppDao;
import com.example.truyentranhthanhxuan.models.TaiKhoan;
import com.example.truyentranhthanhxuan.models.Truyen;
import com.example.truyentranhthanhxuan.models.ChuongTruyen;
import com.example.truyentranhthanhxuan.models.BinhLuan;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //hiển thanh điều hướng dưới cùng
        setContentView(R.layout.activity_main);
        //gọi màn hình trang chủ
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    //thay thế khung fragment trống = home fragment
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }
        //ánh xạ các textview ở thanh điều hướng vào các biến
        TextView navHome = findViewById(R.id.nav_home);
        TextView navSearch = findViewById(R.id.nav_search);
        TextView navLibrary = findViewById(R.id.nav_library);
        TextView navProfile = findViewById(R.id.nav_profile);
        //sự kiện click của các textview
        navHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new HomeFragment())
                        .commit();
            }
        });
        navSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new SearchFragment())
                        .commit();
            }
        });
        navLibrary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new LibraryFragment())
                        .commit();
            }
        });
        navProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new AccountFragment())
                        .commit();
            }
        });
        /*
        // ==================
        // CODE TEST DATABASE
        // ==================
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            AppDao dao = db.appDao();

            // 1. Khởi tạo/Đăng nhập Admin
            TaiKhoan testAdmin = dao.login("admin", "123456");
            int idAdmin;
            if (testAdmin == null) {
                idAdmin = (int) dao.insertTaiKhoan(new TaiKhoan("admin", "123456", 1));
            } else {
                idAdmin = testAdmin.id;
            }

            // 2. Admin thêm bộ truyện mới
            long truyenIdMoi = dao.insertTruyen(new Truyen("Xuyên không về năm 36", "Thể loại xuyên không học đường", 1));
            int idTruyen = (int) truyenIdMoi;

            // 3. Admin thêm Chương
            dao.insertChuong(new ChuongTruyen(idTruyen, 1f, "Chương 1: Khởi đầu", "Nội dung chương 1..."));
            dao.insertChuong(new ChuongTruyen(idTruyen, 2f, "Chương 2: Biến cố", "Nội dung chương 2..."));

            // 4. User bình luận
            dao.insertBinhLuan(new BinhLuan(idAdmin, idTruyen, "Truyện này nét vẽ đẹp quá admin Khang Duy đẹp trai ơi!", "12/05/2026 21:00"));
            dao.insertBinhLuan(new BinhLuan(idAdmin, idTruyen, "Hóng chương mới từng ngày ><", "12/05/2026 21:05"));

            // 5. In kết quả ra Logcat
            Log.d("TEST_DB", "================ KẾT QUẢ DATABASE ================");

            List<Truyen> danhSachTruyen = dao.getTruyenHoatDong();
            for (Truyen t : danhSachTruyen) {
                Log.d("TEST_DB", "[TRUYỆN] " + t.tenTruyen);

                List<ChuongTruyen> danhSachChuong = dao.getDanhSachChuong(t.id);
                for (ChuongTruyen c : danhSachChuong) {
                    Log.d("TEST_DB", "   -> " + c.tenChuong);
                }

                List<BinhLuan> danhSachBL = dao.getBinhLuanTheoTruyen(t.id);
                for (BinhLuan bl : danhSachBL) {
                    Log.d("TEST_DB", "   => [Bình luận] User " + bl.maTk + " nói: " + bl.noiDung);
                }
            }
            Log.d("TEST_DB", "==================================================");
        });
        */

    }
}