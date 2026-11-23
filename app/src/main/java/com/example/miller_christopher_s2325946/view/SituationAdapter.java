package com.example.miller_christopher_s2325946.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.miller_christopher_s2325946.R;
import com.example.miller_christopher_s2325946.model.Situation;

import java.util.ArrayList;

/**
 * custom listview adapter that binds each situation to a row with its icon, summary, delay badge, and distance
 */
public class SituationAdapter extends BaseAdapter
{
    Context context;
    ArrayList<Situation> situations;
    String selectedId;
    LayoutInflater inflter;

    public SituationAdapter(Context applicationContext, ArrayList<Situation> situations,
                            String selectedId) {
        this.context = applicationContext;
        this.situations = situations;
        this.selectedId = selectedId;
        inflter = (LayoutInflater.from(applicationContext));
    }

    @Override
    public int getCount() {
        return situations.size();
    }

    @Override
    public Situation getItem(int i) {
        return situations.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    public void setSelectedId(String selectedId) {
        this.selectedId = selectedId;
        notifyDataSetChanged();
    }

    // builds or recycles a list row using viewholder pattern
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        WidgetsHolder holder = null;
        if (view == null) {
            view = inflter.inflate(R.layout.list_item_situation, null);
            holder = new WidgetsHolder();
            holder.icon = (ImageView) view.findViewById(R.id.itemIcon);
            holder.type = (TextView) view.findViewById(R.id.itemType);
            holder.comment = (TextView) view.findViewById(R.id.itemComment);
            holder.badge = (TextView) view.findViewById(R.id.itemBadge);
            holder.distance = (TextView) view.findViewById(R.id.itemDistance);
            view.setTag(holder);
        }
        else holder = (WidgetsHolder) view.getTag();

        Situation situation = situations.get(i);

        holder.icon.setImageResource(SituationIcons.getIcon(situation));

        String type = situation.getTypeLabel();
        if (!situation.getRoadsText().isEmpty()) {
            type += "  •  " + situation.getRoadsText();
        }
        holder.type.setText(type);
        holder.comment.setText(situation.getComment());

        holder.badge.setText(SituationIcons.getBadgeText(situation));
        holder.badge.setBackgroundColor(
                context.getResources().getColor(SituationIcons.getBadgeColour(situation), null));

        if (situation.getDistanceMiles() >= 0) {
            holder.distance.setText(String.format("%.1f miles away", situation.getDistanceMiles()));
        } else {
            holder.distance.setText("");
        }

        if (situation.getId().equals(selectedId)) {
            view.setBackgroundColor(context.getResources().getColor(R.color.item_selected, null));
        } else {
            view.setBackgroundColor(context.getResources().getColor(R.color.item_normal, null));
        }
        return view;
    }

    static class WidgetsHolder {
        ImageView icon;
        TextView type;
        TextView comment;
        TextView badge;
        TextView distance;
    }
}
