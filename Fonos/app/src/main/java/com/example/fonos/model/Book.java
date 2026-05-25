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
        this.isTrending = false;
        this.isNewRelease = false;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }


    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }


    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }


    public int getChapterCount() {
        return chapterCount;
    }

    public void setChapterCount(int chapterCount) {
        this.chapterCount = chapterCount;
    }


    public int getCoverDrawableRes() {
        return coverDrawableRes;
    }

    public void setCoverDrawableRes(int coverDrawableRes) {
        this.coverDrawableRes = coverDrawableRes;
    }


    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }


    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }


    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }


    public boolean getIsTrending() {
        return isTrending;
    }

    public void setIsTrending(boolean trending) {
        isTrending = trending;
    }

    public boolean isTrending() {
        return isTrending;
    }


    public boolean getIsNewRelease() {
        return isNewRelease;
    }

    public void setIsNewRelease(boolean newRelease) {
        isNewRelease = newRelease;
    }

    public boolean isNewRelease() {
        return isNewRelease;
    }
}