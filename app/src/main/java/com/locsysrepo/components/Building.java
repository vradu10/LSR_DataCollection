package com.locsysrepo.components;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by valentin
 */

public class Building {
    public String name;
    private String[] floorNames;
    private int[] drawables = null;
    private LatLng center;
    private float width;
    private boolean hasDrawable = false;

    public Building(String name) {
        this.name = name;
    }

    public Building(String name, String[] floorNames) {
        this.name = name;
        this.floorNames = floorNames;
    }

    public void setDrawables(int[] drawables) {
        this.drawables = drawables;
        this.hasDrawable = true;
    }

    public boolean hasDrawings() {
        return this.hasDrawable;
    }

    public int getDrawableId(int floorIndex) {
        if (drawables!= null && floorIndex < floorNames.length)
            return drawables[floorIndex];
        else
            return -100;
    }

    public int getFloorIndex(String floorName) {
        for (int i = 0; i < floorName.length(); i++)
            if (floorName.equals(this.floorNames[i]))
                return i;
        return -100; // error number
    }

    public String getFloorName(int floorIndex) {
        if (floorIndex < floorNames.length && floorIndex >= 0)
            return floorNames[floorIndex];
        return null;
    }

    public String[] getFloors() {
        return this.floorNames;
    }
    public int getNumberOfFloors() {
        return this.floorNames.length;
    }

    public float getWidth() { return this.width; }
    public LatLng getCenter() { return this.center; }

    public void setCenterAndWidth(LatLng latLng, float width) {
        this.center = latLng;
        this.width = width;
    }
}
