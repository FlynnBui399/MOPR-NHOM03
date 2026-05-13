package com.example.fonos.model;

public class Book {
    private int id;
    private String title;
    private String author;
    private String description;
    private float rating;
    private String duration;
    private int chapterCount;
    private int coverDrawableRes;
    private String category;

    public Book(int id, String title, String author, String description,
                float rating, String duration, int chapterCount,
                int coverDrawableRes, String category) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.description = description;
        this.rating = rating;
        this.duration = duration;
        this.chapterCount = chapterCount;
        this.coverDrawableRes = coverDrawableRes;
        this.category = category;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getDescription() { return description; }
    public float getRating() { return rating; }
    public String getDuration() { return duration; }
    public int getChapterCount() { return chapterCount; }
    public int getCoverDrawableRes() { return coverDrawableRes; }
    public String getCategory() { return category; }
}
