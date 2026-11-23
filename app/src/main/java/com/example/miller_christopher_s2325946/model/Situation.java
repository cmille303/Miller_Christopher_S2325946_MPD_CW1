package com.example.miller_christopher_s2325946.model;

import android.location.Location;
import android.text.TextUtils;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * model for a single traffic scotland incident or roadworks entry, plus helpers for road matching, distance, and display text
 */
public class Situation
{
    public static final int CAT_INCIDENT = 0;
    public static final int CAT_ROAD_CLOSED = 1;
    public static final int CAT_LANE_CLOSURE = 2;
    public static final int CAT_QUEUE = 3;
    public static final int CAT_ROADWORKS = 4;
    public static final int CAT_TRAFFIC_LIGHTS = 5;
    public static final int CAT_VERGE_WORKS = 6;
    public static final int CAT_SIGNAGE = 7;

    private boolean roadworks;
    private String id = "";
    private String recordType = "";
    private String subType = "";
    private String comment = "";
    private String direction = "";
    private String probability = "";
    private String sourceName = "";
    private String validityStatus = "";
    private String complianceOption = "";
    private boolean urgentRoadworks = false;
    private Date creationTime;
    private Date versionTime;
    private Date startTime;
    private Date endTime;
    private Impact impact;
    private ArrayList<LocationPoint> locations;

    private ArrayList<String> roads;
    private int category = CAT_INCIDENT;

    // -1 means distance hasn't been worked out yet
    private double distanceMiles = -1;

    public Situation() {
        impact = new Impact();
        locations = new ArrayList<>();
        roads = new ArrayList<>();
    }

    public boolean isRoadworks() {
        return roadworks;
    }

    public void setRoadworks(boolean roadworks) {
        this.roadworks = roadworks;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRecordType() {
        return recordType;
    }

    public void setRecordType(String recordType) {
        this.recordType = recordType;
    }

    public String getSubType() {
        return subType;
    }

    public void setSubType(String subType) {
        this.subType = subType.trim();
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment.trim();
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction.trim();
    }

    public String getProbability() {
        return probability;
    }

    public void setProbability(String probability) {
        this.probability = probability.trim();
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName.trim();
    }

    public String getValidityStatus() {
        return validityStatus;
    }

    public void setValidityStatus(String validityStatus) {
        this.validityStatus = validityStatus.trim();
    }

    public String getComplianceOption() {
        return complianceOption;
    }

    public void setComplianceOption(String complianceOption) {
        this.complianceOption = complianceOption.trim();
    }

    public boolean isUrgentRoadworks() {
        return urgentRoadworks;
    }

    public void setUrgentRoadworks(String text) {
        this.urgentRoadworks = text.trim().equalsIgnoreCase("true");
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(String text) {
        this.creationTime = parseDate(text);
    }

    public Date getVersionTime() {
        return versionTime;
    }

    public void setVersionTime(String text) {
        this.versionTime = parseDate(text);
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(String text) {
        this.startTime = parseDate(text);
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(String text) {
        this.endTime = parseDate(text);
    }

    public Impact getImpact() {
        return impact;
    }

    public ArrayList<LocationPoint> getLocations() {
        return locations;
    }

    public void addLocation(LocationPoint point) {
        locations.add(point);
    }

    public ArrayList<String> getRoads() {
        return roads;
    }

    public int getCategory() {
        return category;
    }

    public double getDistanceMiles() {
        return distanceMiles;
    }

    public void setDistanceMiles(double distanceMiles) {
        this.distanceMiles = distanceMiles;
    }

    // parses ISO date strings from the XML feed, returns null if malformed
    private Date parseDate(String text) {
        try {
            SimpleDateFormat feedFormat =
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.UK);
            return feedFormat.parse(text.trim());
        } catch (ParseException e) {
            Log.e("Situation", "Error parsing date: " + text);
            return null;
        }
    }

    public static String formatDate(Date date) {
        if (date == null) {
            return "Not specified";
        }
        SimpleDateFormat displayFormat = new SimpleDateFormat("EEE d MMM yyyy, HH:mm", Locale.UK);
        return displayFormat.format(date);
    }

    // called once </situation> is reached to work out the road list and icon category
    public void finishParsing() {
        buildRoadList();
        if (roadworks) {
            category = findRoadworksCategory();
        } else {
            category = findIncidentCategory();
        }
    }

    // pulls out a UK road code from the start of a word (A/B/M + digits, plus optional motorway suffix)
    public static String extractRoadName(String word) {
        String w = word.trim().toUpperCase();
        if (w.length() < 2) {
            return "";
        }
        char first = w.charAt(0);
        if (first != 'A' && first != 'B' && first != 'M') {
            return "";
        }
        int i = 1;
        while (i < w.length() && Character.isDigit(w.charAt(i))) {
            i++;
        }
        if (i == 1) {
            return "";
        }
        String road = w.substring(0, i);
        // normalise motorway-standard A-roads so A74M and A74(M) match
        if (w.startsWith("(M)", i)) {
            road += "(M)";
        } else if (w.startsWith("M", i)
                && (i + 1 == w.length() || !Character.isLetter(w.charAt(i + 1)))) {
            road += "(M)";
        }
        return road;
    }

    private void addRoad(String road) {
        if (!road.isEmpty() && !roads.contains(road)) {
            roads.add(road);
        }
    }

    // collects road names from both the location points and the comment text
    private void buildRoadList() {
        roads.clear();
        for (LocationPoint point : locations) {
            for (String name : point.getRoadNames()) {
                addRoad(extractRoadName(name));
            }
        }
        String[] words = comment.split("[ /]+");
        for (String word : words) {
            if (word.startsWith("(")) {
                word = word.substring(1);
            }
            addRoad(extractRoadName(word));
        }
    }

    // picks an icon category for unplanned incidents based on subtype and lane impact
    private int findIncidentCategory() {
        String type = subType.toLowerCase();
        if (type.contains("closed") || impact.getOperationalLanes() == 0) {
            return CAT_ROAD_CLOSED;
        } else if (recordType.equalsIgnoreCase("AbnormalTraffic")) {
            return CAT_QUEUE;
        } else if (type.contains("lane") || impact.getLanesRestricted() > 0) {
            return CAT_LANE_CLOSURE;
        } else {
            return CAT_INCIDENT;
        }
    }

    // the feed just labels all roadworks as "roadworks", so guess a more specific category from the comment
    private int findRoadworksCategory() {
        // wrap in spaces so whole-word checks like " ttl " don't match inside words like "little"
        String text = " " + comment.toLowerCase().replaceAll("[^a-z0-9]+", " ") + " ";

        // check order matters since a comment can mention multiple keywords
        if (text.contains(" ttl ") || text.contains(" ttls ")
                || text.contains("traffic light") || text.contains("temp lights")
                || text.contains("stop and go") || text.contains("stop go")
                || text.contains("convoy")) {
            return CAT_TRAFFIC_LIGHTS;
        } else if (text.contains(" mvms ") || text.contains("signage") || text.contains(" signs ")) {
            return CAT_SIGNAGE;
        } else if (text.contains("closure") || text.contains(" closed ")) {
            if (text.contains("footway") || text.contains("footpath") || text.contains("cycle")) {
                return CAT_ROADWORKS;
            } else if (text.contains(" lane ") || text.contains(" lanes ")
                    || text.contains("shoulder") || text.contains("layby") || text.contains(" mlc ")) {
                return CAT_LANE_CLOSURE;
            } else {
                return CAT_ROAD_CLOSED;
            }
        } else if (text.contains("verge")) {
            return CAT_VERGE_WORKS;
        } else if (text.contains(" mlc ") || text.contains(" mlcs ")
                || text.contains("contraflow") || text.contains("narrow lanes")) {
            return CAT_LANE_CLOSURE;
        } else {
            return CAT_ROADWORKS;
        }
    }

    // turns camelCase values from the feed into readable text (e.g. "roadClosed" -> "Road closed")
    public static String splitCamelCase(String text) {
        String result = "";
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (i == 0) {
                result += Character.toUpperCase(c);
            } else if (Character.isUpperCase(c)) {
                result += " " + Character.toLowerCase(c);
            } else {
                result += c;
            }
        }
        return result;
    }

    public String getCategoryLabel() {
        switch (category) {
            case CAT_ROAD_CLOSED:
                return "Road closed";
            case CAT_LANE_CLOSURE:
                return "Lane closure";
            case CAT_QUEUE:
                return "Queuing traffic";
            case CAT_ROADWORKS:
                return "Roadworks";
            case CAT_TRAFFIC_LIGHTS:
                return "Temporary traffic lights";
            case CAT_VERGE_WORKS:
                return "Verge works";
            case CAT_SIGNAGE:
                return "Signage";
            default:
                return "Incident";
        }
    }

    public String getTypeLabel() {
        if (roadworks) {
            if (category == CAT_ROADWORKS) {
                return "Roadworks";
            } else {
                return "Roadworks: " + getCategoryLabel();
            }
        } else {
            if (!subType.isEmpty()) {
                return "Incident: " + splitCamelCase(subType);
            } else if (!recordType.isEmpty()) {
                return "Incident: " + splitCamelCase(recordType);
            } else {
                return "Incident";
            }
        }
    }

    public String getRoadsText() {
        return TextUtils.join(", ", roads);
    }

    public String getDirectionText() {
        if (direction.isEmpty()) {
            return "Not specified";
        } else if (direction.equalsIgnoreCase("bothWays")) {
            return "Both directions";
        } else if (direction.toLowerCase().endsWith("bound")) {
            return direction.substring(0, 1).toUpperCase() + direction.substring(1).toLowerCase();
        } else {
            return splitCamelCase(direction);
        }
    }

    public String getDurationText() {
        if (startTime == null || endTime == null) {
            return "Not specified";
        }
        long millis = endTime.getTime() - startTime.getTime();
        long hours = millis / (1000 * 60 * 60);
        long days = hours / 24;
        if (hours < 1) {
            return "Less than 1 hour";
        } else if (hours < 48) {
            return hours + " hours";
        } else {
            return days + " days";
        }
    }

    // explains common traffic scotland abbreviations if they show up in the comment
    public String getAbbreviationsText() {
        String text = " " + comment.toLowerCase().replaceAll("[^a-z0-9]+", " ") + " ";
        String result = "";
        if (text.contains(" ttl ") || text.contains(" ttls ")) {
            result += "TTL = temporary traffic lights\n";
        }
        if (text.contains(" mlc ") || text.contains(" mlcs ")) {
            result += "MLC = mobile lane closure\n";
        }
        if (text.contains(" mvms ")) {
            result += "MVMS = mobile variable message sign\n";
        }
        if (text.contains(" rbt ")) {
            result += "Rbt = roundabout\n";
        }
        if (text.contains(" jct ")) {
            result += "Jct = junction\n";
        }
        if (text.contains(" nb ") || text.contains(" n b ") || text.contains(" sb ")
                || text.contains(" s b ") || text.contains(" eb ") || text.contains(" e b ")
                || text.contains(" wb ") || text.contains(" w b ")) {
            result += "NB / SB / EB / WB = north / south / east / westbound\n";
        }
        return result.trim();
    }

    public boolean hasLocation() {
        for (LocationPoint point : locations) {
            if (point.hasCoordinates()) {
                return true;
            }
        }
        return false;
    }

    // averages the point latitudes to get the midpoint for the map marker
    public double getLatitude() {
        double sum = 0;
        int count = 0;
        for (LocationPoint point : locations) {
            if (point.hasCoordinates()) {
                sum += point.getLatitude();
                count++;
            }
        }
        if (count == 0) {
            return 0;
        }
        return sum / count;
    }

    // averages the point longitudes to get the midpoint for the map marker
    public double getLongitude() {
        double sum = 0;
        int count = 0;
        for (LocationPoint point : locations) {
            if (point.hasCoordinates()) {
                sum += point.getLongitude();
                count++;
            }
        }
        if (count == 0) {
            return 0;
        }
        return sum / count;
    }

    // distance in miles to whichever end (from/to) is closest, or -1 if there are no coords
    public double distanceTo(double latitude, double longitude) {
        double closest = -1;
        for (LocationPoint point : locations) {
            if (point.hasCoordinates()) {
                double d = distanceBetween(latitude, longitude,
                        point.getLatitude(), point.getLongitude());
                if (closest < 0 || d < closest) {
                    closest = d;
                }
            }
        }
        return closest;
    }

    public static double distanceBetween(double lat1, double lon1, double lat2, double lon2) {
        float[] results = new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        return results[0] / 1609.344;
    }

    // matches exact road codes so "A9" doesn't match "A90", or falls back to searching comment/location text
    public boolean matchesRoad(String query) {
        String q = query.trim().toUpperCase();
        if (q.isEmpty()) {
            return true;
        }
        String noSpaces = q.replace(" ", "");
        String asRoad = extractRoadName(noSpaces);
        if (!asRoad.isEmpty() && noSpaces.length() <= asRoad.length()) {
            for (String road : roads) {
                if (road.equals(asRoad)
                        || road.equals(asRoad + "(M)")
                        || asRoad.equals(road + "(M)")) {
                    return true;
                }
            }
            return false;
        } else {
            if (comment.toUpperCase().contains(q)) {
                return true;
            }
            for (LocationPoint point : locations) {
                if (point.getDescription().toUpperCase().contains(q)) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public String toString() {
        return "Situation{" +
                "roadworks=" + roadworks +
                ", id='" + id + '\'' +
                ", recordType='" + recordType + '\'' +
                ", subType='" + subType + '\'' +
                ", category=" + getCategoryLabel() +
                ", roads=" + roads +
                ", comment='" + comment + '\'' +
                ", direction='" + direction + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", impact=" + impact +
                ", locations=" + locations +
                '}';
    }
}
