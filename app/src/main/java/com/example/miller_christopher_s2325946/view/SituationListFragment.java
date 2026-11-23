package com.example.miller_christopher_s2325946.view;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.example.miller_christopher_s2325946.R;
import com.example.miller_christopher_s2325946.model.Situation;

import java.util.ArrayList;

/**
 * displays the scrollable list of filtered situations along with the total counts, and keeps scroll position across rotations
 */
public class SituationListFragment extends Fragment implements AdapterView.OnItemClickListener
{
    public interface ListListener {
        void onSituationSelected(Situation situation);
    }

    private static final String STATE_POSITION = "listPosition";

    private ListListener listener;
    private ListView listView;
    private TextView countText;
    private TextView emptyText;
    private SituationAdapter adapter;

    private ArrayList<Situation> situations = new ArrayList<>();
    private String selectedId = "";
    private int positionToRestore = -1;

    public SituationListFragment() {}

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof ListListener)
            listener = (ListListener) context;
        else
            throw new RuntimeException(context.toString() + " must implement ListListener!");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_list, container, false);
        listView = (ListView) v.findViewById(R.id.situationList);
        countText = (TextView) v.findViewById(R.id.countText);
        emptyText = (TextView) v.findViewById(R.id.emptyText);
        listView.setOnItemClickListener(this);

        if (savedInstanceState != null) {
            positionToRestore = savedInstanceState.getInt(STATE_POSITION, -1);
        }
        updateList();
        return v;
    }

    // saves scroll position before screen rotation
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (listView != null) {
            outState.putInt(STATE_POSITION, listView.getFirstVisiblePosition());
        }
    }

    public void showSituations(ArrayList<Situation> newSituations) {
        situations = newSituations;
        updateList();
    }

    public void showSelected(Situation situation) {
        if (situation == null) {
            selectedId = "";
        } else {
            selectedId = situation.getId();
        }
        if (adapter != null) {
            adapter.setSelectedId(selectedId);
        }
    }

    // refreshes the listview, restores scroll position if needed, and updates the header count
    private void updateList() {
        if (listView == null) {
            return;
        }
        adapter = new SituationAdapter(getContext(), situations, selectedId);
        listView.setAdapter(adapter);

        if (positionToRestore >= 0 && situations.size() > 0) {
            listView.setSelection(positionToRestore);
            positionToRestore = -1;
        }

        int incidents = 0;
        for (Situation s : situations) {
            if (!s.isRoadworks()) {
                incidents++;
            }
        }
        countText.setText("Showing " + situations.size() + " situations: " + incidents
                + " incidents, " + (situations.size() - incidents) + " roadworks");
        if (situations.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
        } else {
            emptyText.setVisibility(View.GONE);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        listener.onSituationSelected(adapter.getItem(position));
    }
}
