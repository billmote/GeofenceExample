package com.androidfu.example.geofences;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by bmote on 2/4/15.
 */
public class MyPlaces {
    private String title;
    private String snippet;
    private LatLng coordinates;
    private float fenceRadius;
    private int iconResourceId;
    private float defaultZoomLevel;

    public MyPlaces(String title, String snippet, LatLng coordinates, float fenceRadius, int defaultZoomLevel, int iconResourceId) {
        this.title = title;
        this.snippet = snippet;
        this.coordinates = coordinates;
        this.fenceRadius = fenceRadius;
        this.defaultZoomLevel = defaultZoomLevel;
        this.iconResourceId = iconResourceId;
    }

    public String getTitle() {
        return title;
    }

    public String getSnippet() {
        return snippet;
    }

    public LatLng getCoordinates() {
        return coordinates;
    }

    public float getFenceRadius() {
        return fenceRadius;
    }

    public float getDefaultZoomLevel() {
        return defaultZoomLevel;
    }

    public int getIconResourceId() {
        return iconResourceId;
    }
}
