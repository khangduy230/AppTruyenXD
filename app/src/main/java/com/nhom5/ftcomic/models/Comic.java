package com.nhom5.ftcomic.models;

public class Comic {
    private int id;
    private String title;
    private String author;
    private String description;
    private int image;
    private String likes;
    private String rating;
    private String comments;
    private String name;

    public Comic(int id, String title, String author, String description, int image,
                 String likes, String rating, String comments) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.description = description;
        this.image = image;
        this.likes = likes;
        this.rating = rating;
        this.comments = comments;
    }

    public Comic(int image, String name) {
        this.image = image;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getDescription() {
        return description;
    }

    public int getImage() {
        return image;
    }

    public String getLikes() {
        return likes;
    }

    public String getRating() {
        return rating;
    }

    public String getComments() {
        return comments;
    }
    public String getName() {
        return name;
    }
}