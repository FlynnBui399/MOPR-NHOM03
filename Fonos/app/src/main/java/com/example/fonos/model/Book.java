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
    private String coverUrl;
    private String audioUrl;
    private boolean isTrending;
    private boolean isNewRelease;

    public Book() {
        // Required empty constructor for Firestore deserialization
    }

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
        this.coverUrl = "";
        this.audioUrl = "";
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

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }

    // Firestore deserialization mapping
    public boolean getIsTrending() { return isTrending; }
    public void setIsTrending(boolean isTrending) { this.isTrending = isTrending; }
    public boolean getIsNewRelease() { return isNewRelease; }
    public void setIsNewRelease(boolean isNewRelease) { this.isNewRelease = isNewRelease; }

    // Backward compatible helper methods for application code
    public boolean isTrending() { return isTrending; }
    public boolean isNewRelease() { return isNewRelease; }
}
