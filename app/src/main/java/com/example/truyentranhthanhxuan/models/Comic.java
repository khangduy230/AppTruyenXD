package com.example.truyentranhthanhxuan.models;

public class Comic {
    private int image; // Lưu ID của ảnh trong drawable (VD: R.drawable.my_image)
    private String name; // Tên truyện (để truyền sang Activity kia)

    public Comic(int image, String name) {
        this.image = image;
        this.name = name;
    }

    public int getImage() {
        return image;
    }

    public String getName() {
        return name;
    }
}
