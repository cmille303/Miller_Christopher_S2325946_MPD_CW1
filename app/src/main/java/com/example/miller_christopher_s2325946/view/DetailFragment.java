package com.example.miller_christopher_s2325946.view;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.example.miller_christopher_s2325946.R;
import com.example.miller_christopher_s2325946.model.LocationPoint;
import com.example.miller_christopher_s2325946.model.Situation;

import java.util.ArrayList;

/**
 * shows the full breakdown for whichever situation is currently selected (location, times, impact, and extra info)
 */
public class DetailFragment extends Fragment implements View.OnClickListener
{
    public interface DetailListener {
        void onShowOnMap();
    }

    private DetailListener listener;
    private Situation situation;
    private String placeName = "";

    private TextView noSelectionText;
    private View detailContent;
    private ImageView detailIcon;
    private TextView detailType;
    private TextView detailComment;
    private TextView detailBadge;
    private TextView detailWhere;
    private TextView detailWhen;
    private TextView detailImpact;
    private TextView detailMore;
    private Button showOnMapButton;

    public DetailFragment() {}

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof DetailListener)
            listener = (DetailListener) context;
        else
            throw new RuntimeException(context.toString() + " must implement DetailListener!");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_detail, container, false);
        noSelectionText = (TextView) v.findViewById(R.id.noSelectionText);
        detailContent = v.findViewById(R.id.detailContent);
        detailIcon = (ImageView) v.findViewById(R.id.detailIcon);
        detailType = (TextView) v.findViewById(R.id.detailType);
        detailComment = (TextView) v.findViewById(R.id.detailComment);
        detailBadge = (TextView) v.findViewById(R.id.detailBadge);
        detailWhere = (TextView) v.findViewById(R.id.detailWhere);
        detailWhen = (TextView) v.findViewById(R.id.detailWhen);
        detailImpact = (TextView) v.findViewById(R.id.detailImpact);
        detailMore = (TextView) v.findViewById(R.id.detailMore);
        showOnMapButton = (Button) v.findViewById(R.id.showOnMapButton);
        showOnMapButton.setOnClickListener(this);

        updateDetails();
        return v;
    }

    public void showSituation(Situation newSituation, String newPlaceName) {
        situation = newSituation;
        placeName = newPlaceName;
        updateDetails();
    }

    @Override
    public void onClick(View v) {
        if (v == showOnMapButton && situation != null) {
            listener.onShowOnMap();
        }
    }

    // populates all the detail sections, or shows the empty hint if nothing is selected
    private void updateDetails() {
        if (detailContent == null) {
            return;
        }
        if (situation == null) {
            noSelectionText.setVisibility(View.VISIBLE);
            detailContent.setVisibility(View.GONE);
            return;
        }
        noSelectionText.setVisibility(View.GONE);
        detailContent.setVisibility(View.VISIBLE);

        detailIcon.setImageResource(SituationIcons.getIcon(situation));
        detailType.setText(situation.getTypeLabel());
        detailComment.setText(situation.getComment());
        detailBadge.setText(SituationIcons.getBadgeText(situation));
        detailBadge.setBackgroundColor(
                getResources().getColor(SituationIcons.getBadgeColour(situation), null));

        String where = "";
        if (!situation.getRoadsText().isEmpty()) {
            where += "Road: " + situation.getRoadsText() + "\n";
        }
        where += "Direction: " + situation.getDirectionText() + "\n";
        ArrayList<LocationPoint> points = situation.getLocations();
        for (LocationPoint point : points) {
            String role = point.getRole().substring(0, 1).toUpperCase() + point.getRole().substring(1);
            where += role + ": " + point.getDisplayText();
            if (point.hasCoordinates()) {
                where += String.format(" (%.5f, %.5f)", point.getLatitude(), point.getLongitude());
            }
            where += "\n";
        }
        if (points.size() == 2 && points.get(0).hasCoordinates() && points.get(1).hasCoordinates()) {
            double length = Situation.distanceBetween(
                    points.get(0).getLatitude(), points.get(0).getLongitude(),
                    points.get(1).getLatitude(), points.get(1).getLongitude());
            where += String.format("Length affected: %.1f miles in a straight line\n", length);
        }
        if (situation.getDistanceMiles() >= 0) {
            where += String.format("Distance from %s: %.1f miles", placeName, situation.getDistanceMiles());
        } else {
            where += "Position not available";
        }
        detailWhere.setText(where.trim());

        String when = "Start: " + Situation.formatDate(situation.getStartTime()) + "\n";
        if (situation.getEndTime() != null) {
            when += "End: " + Situation.formatDate(situation.getEndTime()) + "\n";
            when += "Duration: " + situation.getDurationText();
        } else {
            when += "End: not known yet";
        }
        detailWhen.setText(when);

        String impact = "";
        if (situation.isRoadworks()) {
            impact += "Delays: " + situation.getImpact().getDelayText() + "\n";
            impact += "Type of works: " + situation.getCategoryLabel() + "\n";
            impact += "Urgent roadworks: " + (situation.isUrgentRoadworks() ? "Yes" : "No") + "\n";
        } else {
            if (!situation.getImpact().getLanesText().isEmpty()) {
                impact += "Lanes: " + situation.getImpact().getLanesText() + "\n";
            }
            impact += "Kind of event: " + Situation.splitCamelCase(situation.getRecordType()) + "\n";
            if (!situation.getComplianceOption().isEmpty()) {
                impact += "Compliance: " + Situation.splitCamelCase(situation.getComplianceOption()) + "\n";
            }
        }
        if (!situation.getAbbreviationsText().isEmpty()) {
            impact += situation.getAbbreviationsText();
        }
        detailImpact.setText(impact.trim());

        String more = "Reference: " + situation.getId() + "\n";
        more += "Source: " + situation.getSourceName() + "\n";
        if (!situation.getProbability().isEmpty()) {
            more += "Probability: " + Situation.splitCamelCase(situation.getProbability()) + "\n";
        }
        more += "First reported: " + Situation.formatDate(situation.getCreationTime()) + "\n";
        more += "Last updated: " + Situation.formatDate(situation.getVersionTime());
        detailMore.setText(more);
    }
}
