package com.locsysrepo.components;

import com.google.android.gms.maps.model.LatLng;

/**
 * Elementary point unit represented by latitude, longitude (plus altitude and/or floor)
 *
 * This must be immutable so no setters are defined
 *
 * Author: Valentin Radu
 *
 */

public class Point3D {

    /**
     * Latitude and longitude, in the same metrics as LatLng coordinates in GMaps
     */
    private double lat, lng;

    /**
     * Floor level in the building with 0 being ground floor
     */
    private int floor = 0;
    private boolean hasFloor = false;	// indicates if this point has a floor value

    /**
     * Altitude in meters as returned by the GPS estimation, considered from the see level
     */
    private double altitude = 0;
    private boolean hasAltitude = false;	// indicates if this point has an altitude level

    /**
     * Point3D constructor, using just two dimension coordinates
     */
    public Point3D(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    /**
     * Point3D constructor from LatLng
     */
    public Point3D(LatLng latLng) {
        this.lat = latLng.latitude;
        this.lng = latLng.longitude;
    }

    /**
     * Point3D constructor, taking the altitude as well as the other two dimensions
     */
    public Point3D(double lat, double lng, double alt) {
        this(lat, lng);
        this.altitude = alt;
        this.hasAltitude = true;
    }

    /**
     * Point3D constructor, taking the floor as the third dimension.
     */
    public Point3D(double lat, double lng, int floor) {
        this(lat, lng);
        this.floor = floor;
        this.hasFloor = true;
    }

    /**
     * Constructor taking all the attributes of a point3D
     */
    public Point3D(double lat, double lng, double altitude, int floor) {
        this(lat, lng);

        this.altitude = altitude;
        this.hasAltitude = true;

        this.floor = floor;
        this.hasFloor = true;
    }

    public double getLat() {
        return this.lat;
    }
    public double getLng() {
        return this.lng;
    }

    public boolean hasAltitude() {
        return this.hasAltitude;
    }
    public double getAltitude() {
        return this.altitude;
    }

    public boolean hasFloor() {
        return this.hasFloor;
    }
    public int getFloor() {
        return this.floor;
    }

    public String toString() {
        return "<point lat=" + lat + " lng=" + lng +
                ((hasFloor)?" floor=" + floor: "") +
                ((hasAltitude)?" alt=" + altitude: "") +
                " />";
    }

}