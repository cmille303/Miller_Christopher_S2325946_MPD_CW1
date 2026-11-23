package com.example.miller_christopher_s2325946.model;

import android.util.Log;

/**
 * holds the impact details for a situation (open/restricted lanes for incidents, delay level for roadworks)
 */
public class Impact
{
    public static final int DELAY_NONE = 0;
    public static final int DELAY_NORMAL = 1;
    public static final int DELAY_LONG = 2;
    public static final int DELAY_VERY_LONG = 3;

    // -1 means the tag wasn't in the XML
    private int operationalLanes = -1;
    private int lanesRestricted = -1;
    private String delaysType = "";

    public Impact() {
    }

    public int getOperationalLanes() {
        return operationalLanes;
    }

    public void setOperationalLanes(String text) {
        try {
            this.operationalLanes = Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            Log.e("Impact", "Error parsing operational lanes: " + text);
        }
    }

    public int getLanesRestricted() {
        return lanesRestricted;
    }

    public void setLanesRestricted(String text) {
        try {
            this.lanesRestricted = Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            Log.e("Impact", "Error parsing restricted lanes: " + text);
        }
    }

    public String getDelaysType() {
        return delaysType;
    }

    public void setDelaysType(String delaysType) {
        this.delaysType = delaysType.trim();
    }

    // maps raw XML delay string to a numeric severity level for sorting and badge colours
    public int getDelayLevel() {
        String type = delaysType.toLowerCase();
        if (type.isEmpty()) {
            return DELAY_NONE;
        } else if (type.contains("verylong")) {
            // check this before "long" since "verylong" contains "long" too
            return DELAY_VERY_LONG;
        } else if (type.contains("long")) {
            return DELAY_LONG;
        } else {
            return DELAY_NORMAL;
        }
    }

    public String getDelayText() {
        int level = getDelayLevel();
        if (level == DELAY_VERY_LONG) {
            return "Very long delays";
        } else if (level == DELAY_LONG) {
            return "Long delays";
        } else if (level == DELAY_NORMAL) {
            return "Delays";
        } else {
            return "No delays reported";
        }
    }

    // builds a readable summary of open/restricted lanes, or "" if none reported
    public String getLanesText() {
        String text = "";
        if (operationalLanes == 0) {
            text = "Road closed (no lanes open)";
        } else if (operationalLanes == 1) {
            text = "1 lane open";
        } else if (operationalLanes > 1) {
            text = operationalLanes + " lanes open";
        }
        if (lanesRestricted > 0) {
            if (!text.isEmpty()) {
                text += ", ";
            }
            if (lanesRestricted == 1) {
                text += "1 lane restricted";
            } else {
                text += lanesRestricted + " lanes restricted";
            }
        }
        return text;
    }

    @Override
    public String toString() {
        return "Impact{" +
                "operationalLanes=" + operationalLanes +
                ", lanesRestricted=" + lanesRestricted +
                ", delaysType='" + delaysType + '\'' +
                '}';
    }
}
