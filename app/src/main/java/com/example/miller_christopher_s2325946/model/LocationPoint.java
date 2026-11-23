package com.example.miller_christopher_s2325946.model;

import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;

/**
 * represents one end (from or to) of a situation's location, with coords and road/junction names from TPEG XML
 */
public class LocationPoint
{
    private String role = "";
    private String pointType = "";
    private double latitude;
    private double longitude;
    private boolean latitudeSet = false;
    private boolean longitudeSet = false;
    private ArrayList<String> roadNames;
    private String junctionName = "";
    private String description = "";

    public LocationPoint() {
        roadNames = new ArrayList<>();
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPointType() {
        return pointType;
    }

    public void setPointType(String pointType) {
        this.pointType = pointType;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(String text) {
        try {
            this.latitude = Double.parseDouble(text.trim());
            this.latitudeSet = true;
        } catch (NumberFormatException e) {
            Log.e("LocationPoint", "Error parsing latitude: " + text);
        }
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(String text) {
        try {
            this.longitude = Double.parseDouble(text.trim());
            this.longitudeSet = true;
        } catch (NumberFormatException e) {
            Log.e("LocationPoint", "Error parsing longitude: " + text);
        }
    }

    public boolean hasCoordinates() {
        return latitudeSet && longitudeSet;
    }

    public ArrayList<String> getRoadNames() {
        return roadNames;
    }

    public String getJunctionName() {
        return junctionName;
    }

    public String getDescription() {
        return description;
    }

    // sorts a TPEG name value into roadNames, junctionName, or description depending on its descriptor type
    public void addName(String value, String descriptorType) {
        value = value.trim();
        if (value.isEmpty()) {
            return;
        }
        if (descriptorType.equalsIgnoreCase("linkName")
                || descriptorType.toLowerCase().startsWith("tpegilcname")) {
            if (!roadNames.contains(value)) {
                roadNames.add(value);
            }
        } else if (descriptorType.equalsIgnoreCase("junctionName")) {
            junctionName = value;
        } else {
            if (description.isEmpty()) {
                description = value;
            } else if (!description.contains(value)) {
                description += ", " + value;
            }
        }
    }

    // returns the description if we have one, otherwise stitches together road and junction names
    public String getDisplayText() {
        String text = description;
        if (text.isEmpty()) {
            text = TextUtils.join(" / ", roadNames);
            if (!junctionName.isEmpty()) {
                text += " " + junctionName;
            }
        }
        if (text.trim().isEmpty()) {
            text = "Unnamed point";
        }
        return text.trim();
    }

    @Override
    public String toString() {
        return "LocationPoint{" +
                "role='" + role + '\'' +
                ", pointType='" + pointType + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", roadNames=" + roadNames +
                ", junctionName='" + junctionName + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
