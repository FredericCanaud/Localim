package com.example.localim.models;

import org.osmdroid.util.GeoPoint;

public class Service {
    private String id;
    private String title;
    private String description;
    private String image;
    private String place;
    private GeoPoint position;
    private String tag;

    public Service(String id, String title, String description, String image) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.image = image;
    }

    public Service(String id, String title, String description, String image, GeoPoint geopoint) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.image = image;
        this.position = geopoint;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImage() {
        return image;
    }

    public GeoPoint getPosition() {
        return position;
    }
}





















