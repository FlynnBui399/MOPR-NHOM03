package com.example.fonos.model;

public class Category {
    private int id;
    private String name;
    private boolean isSelected;

    public Category(int id, String name, boolean isSelected) {
        this.id = id;
        this.name = name;
        this.isSelected = isSelected;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }
}
