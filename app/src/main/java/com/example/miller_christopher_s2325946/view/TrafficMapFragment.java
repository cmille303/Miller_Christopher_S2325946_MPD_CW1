package com.example.miller_christopher_s2325946.view;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import com.example.miller_christopher_s2325946.R;
import com.example.miller_christopher_s2325946.model.LocationPoint;
import com.example.miller_christopher_s2325946.model.Situation;

import java.util.ArrayList;

/**
 * google maps fragment that plots the filtered situations as markers and highlights the selected one with a polyline
 */
public class TrafficMapFragment extends Fragment
        implements OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener, View.OnClickListener
{
    public interface MapListener {
        void onMapSituationSelected(Situation situation);
    }

    private MapListener listener;
    private GoogleMap mMap;
    private CheckBox satelliteBox;

    private ArrayList<Situation> situations = new ArrayList<>();
    private Situation selected;
    private ArrayList<Marker> markers = new ArrayList<>();
    private Polyline selectedLine;

    public TrafficMapFragment() {}

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof MapListener)
            listener = (MapListener) context;
        else
            throw new RuntimeException(context.toString() + " must implement MapListener!");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_map, container, false);
        satelliteBox = (CheckBox) v.findViewById(R.id.satelliteBox);
        satelliteBox.setOnClickListener(this);

        // map is nested inside this fragment so we have to use childFragmentManager
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mMap = null;
        markers.clear();
        selectedLine = null;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.setOnInfoWindowClickListener(this);
        setMapType();
        // draw anything that loaded before the map finished initialising
        drawMarkers();
        drawSelected();
    }

    @Override
    public void onClick(View v) {
        if (v == satelliteBox) {
            setMapType();
        }
    }

    private void setMapType() {
        if (mMap == null) {
            return;
        }
        if (satelliteBox.isChecked()) {
            mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        } else {
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }
    }

    public void showSituations(ArrayList<Situation> newSituations) {
        situations = newSituations;
        if (mMap != null) {
            drawMarkers();
            drawSelected();
        }
    }

    public void showSelected(Situation situation) {
        selected = situation;
        if (mMap != null) {
            drawSelected();
        }
    }

    // clears the map and plots a marker for each filtered situation, then frames the camera
    private void drawMarkers() {
        mMap.clear();
        markers.clear();
        selectedLine = null;

        LatLngBounds.Builder bounds = new LatLngBounds.Builder();
        LatLng onlyPosition = null;

        for (Situation s : situations) {
            if (!s.hasLocation()) {
                continue;
            }
            LatLng position = new LatLng(s.getLatitude(), s.getLongitude());
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title(s.getTypeLabel())
                    .snippet(s.getComment())
                    .icon(BitmapDescriptorFactory.fromResource(SituationIcons.getIcon(s)))
                    .anchor(0.5f, 0.5f));
            marker.setTag(s);
            markers.add(marker);
            bounds.include(position);
            onlyPosition = position;
        }

        if (markers.isEmpty()) {
            // default to an overview of scotland when there's nothing to plot
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(56.8, -4.2), 6));
        } else if (markers.size() == 1) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(onlyPosition, 12));
        } else {
            fitCamera(bounds.build());
        }
    }

    // draws a polyline between the selected situation's from/to points, zooms to it, and opens its info window
    private void drawSelected() {
        if (selectedLine != null) {
            selectedLine.remove();
            selectedLine = null;
        }
        if (selected == null || !selected.hasLocation()) {
            return;
        }

        PolylineOptions line = new PolylineOptions();
        for (LocationPoint point : selected.getLocations()) {
            if (point.hasCoordinates()) {
                line.add(new LatLng(point.getLatitude(), point.getLongitude()));
            }
        }
        line.width(14);
        line.color(getResources().getColor(SituationIcons.getBadgeColour(selected), null));
        selectedLine = mMap.addPolyline(line);

        LatLngBounds.Builder bounds = new LatLngBounds.Builder();
        for (LocationPoint point : selected.getLocations()) {
            if (point.hasCoordinates()) {
                bounds.include(new LatLng(point.getLatitude(), point.getLongitude()));
            }
        }
        fitCamera(bounds.build());
        if (mMap.getCameraPosition().zoom > 15) {
            // cap zoom so tiny road stretches don't zoom in way too close
            mMap.moveCamera(CameraUpdateFactory.zoomTo(15));
        }

        for (Marker marker : markers) {
            Situation s = (Situation) marker.getTag();
            if (s != null && s.getId().equals(selected.getId())) {
                marker.showInfoWindow();
            }
        }
    }

    // pass explicit width/height since the map might be hidden in the viewflipper with 0x0 size
    private void fitCamera(LatLngBounds bounds) {
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels / 2;
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, width, height, 100));
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        Situation s = (Situation) marker.getTag();
        if (s != null) {
            listener.onMapSituationSelected(s);
        }
    }
}
