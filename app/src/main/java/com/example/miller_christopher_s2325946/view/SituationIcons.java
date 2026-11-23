package com.example.miller_christopher_s2325946.view;

import com.example.miller_christopher_s2325946.R;
import com.example.miller_christopher_s2325946.model.Situation;
import com.example.miller_christopher_s2325946.model.Impact;

/**
 * shared helper for picking a situation's icon, badge label, and badge colour so the list, details, and map stay consistent
 */
public class SituationIcons
{
    public static int getIcon(Situation situation) {
        switch (situation.getCategory()) {
            case Situation.CAT_ROAD_CLOSED:
                return R.drawable.ic_road_closed;
            case Situation.CAT_LANE_CLOSURE:
                return R.drawable.ic_lane_closure;
            case Situation.CAT_QUEUE:
                return R.drawable.ic_queue;
            case Situation.CAT_ROADWORKS:
                return R.drawable.ic_roadworks;
            case Situation.CAT_TRAFFIC_LIGHTS:
                return R.drawable.ic_traffic_lights;
            case Situation.CAT_VERGE_WORKS:
                return R.drawable.ic_verge_works;
            case Situation.CAT_SIGNAGE:
                return R.drawable.ic_signage;
            default:
                return R.drawable.ic_incident;
        }
    }

    public static String getBadgeText(Situation situation) {
        if (situation.isRoadworks()) {
            return situation.getImpact().getDelayText();
        } else {
            String lanes = situation.getImpact().getLanesText();
            if (lanes.isEmpty()) {
                return "Incident";
            }
            return lanes;
        }
    }

    // picks the badge colour: roadworks use delay severity scale, incidents get their own grey-blue
    public static int getBadgeColour(Situation situation) {
        if (!situation.isRoadworks()) {
            return R.color.badge_incident;
        }
        int level = situation.getImpact().getDelayLevel();
        if (level == Impact.DELAY_VERY_LONG) {
            return R.color.delay_very_long;
        } else if (level == Impact.DELAY_LONG) {
            return R.color.delay_long;
        } else if (level == Impact.DELAY_NORMAL) {
            return R.color.delay_normal;
        } else {
            return R.color.delay_none;
        }
    }
}
