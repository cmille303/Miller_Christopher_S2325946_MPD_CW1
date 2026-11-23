package com.example.miller_christopher_s2325946.view;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;

import androidx.fragment.app.Fragment;

import com.example.miller_christopher_s2325946.R;

/**
 * search and filter controls (type, road name, and distance from a place) that report changes back to MainActivity
 */
public class SearchFragment extends Fragment
        implements View.OnClickListener, AdapterView.OnItemSelectedListener
{
    public interface SearchListener {
        void onSearchChanged(int type, String road, int placeIndex, int distanceIndex);
    }

    private static final String ARG_PLACES = "places";
    private static final String ARG_DISTANCES = "distances";

    private String[] places = {};
    private String[] distances = {};

    private SearchListener listener;
    private RadioButton allButton;
    private RadioButton incidentsButton;
    private RadioButton roadworksButton;
    private EditText roadInput;
    private Button searchButton;
    private Button resetButton;
    private Spinner distanceSpinner;
    private Spinner placeSpinner;

    public SearchFragment() {}

    // passes the spinner options in via bundle so they survive fragment recreation
    public static SearchFragment newInstance(String[] places, String[] distances) {
        SearchFragment fragment = new SearchFragment();
        Bundle args = new Bundle();
        args.putStringArray(ARG_PLACES, places);
        args.putStringArray(ARG_DISTANCES, distances);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            places = getArguments().getStringArray(ARG_PLACES);
            distances = getArguments().getStringArray(ARG_DISTANCES);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof SearchListener)
            listener = (SearchListener) context;
        else
            throw new RuntimeException(context.toString() + " must implement SearchListener!");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_search, container, false);

        allButton = (RadioButton) v.findViewById(R.id.allButton);
        incidentsButton = (RadioButton) v.findViewById(R.id.incidentsButton);
        roadworksButton = (RadioButton) v.findViewById(R.id.roadworksButton);
        roadInput = (EditText) v.findViewById(R.id.roadInput);
        searchButton = (Button) v.findViewById(R.id.searchButton);
        resetButton = (Button) v.findViewById(R.id.resetButton);
        distanceSpinner = (Spinner) v.findViewById(R.id.distanceSpinner);
        placeSpinner = (Spinner) v.findViewById(R.id.placeSpinner);

        ArrayAdapter<String> distanceAdapter = new ArrayAdapter<String>(getContext(),
                android.R.layout.simple_spinner_item, distances);
        distanceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        distanceSpinner.setAdapter(distanceAdapter);
        ArrayAdapter<String> placeAdapter = new ArrayAdapter<String>(getContext(),
                android.R.layout.simple_spinner_item, places);
        placeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        placeSpinner.setAdapter(placeAdapter);

        allButton.setOnClickListener(this);
        incidentsButton.setOnClickListener(this);
        roadworksButton.setOnClickListener(this);
        searchButton.setOnClickListener(this);
        resetButton.setOnClickListener(this);
        distanceSpinner.setOnItemSelectedListener(this);
        placeSpinner.setOnItemSelectedListener(this);
        return v;
    }

    @Override
    public void onClick(View v) {
        if (v == resetButton) {
            roadInput.setText("");
            allButton.setChecked(true);
            distanceSpinner.setSelection(0);
            hideKeyboard();
        }
        else if (v == searchButton) {
            hideKeyboard();
        }
        sendSearch();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        sendSearch();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    // grabs all current filter inputs and sends them up to MainActivity
    private void sendSearch() {
        int type = 0;
        if (incidentsButton.isChecked()) {
            type = 1;
        } else if (roadworksButton.isChecked()) {
            type = 2;
        }
        String road = roadInput.getText().toString().trim();
        listener.onSearchChanged(type, road, placeSpinner.getSelectedItemPosition(),
                distanceSpinner.getSelectedItemPosition());
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager)
                getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(roadInput.getWindowToken(), 0);
    }
}
