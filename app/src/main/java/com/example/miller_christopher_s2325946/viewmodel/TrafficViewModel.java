package com.example.miller_christopher_s2325946.viewmodel;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.miller_christopher_s2325946.model.Situation;
import com.example.miller_christopher_s2325946.model.TrafficRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * holds the app's UI state and live data so it survives rotation, handles filtering/sorting, and runs the auto-update timer
 */
public class TrafficViewModel extends ViewModel implements TrafficRepository.RepositoryListener
{
    public static final int TYPE_ALL = 0;
    public static final int TYPE_INCIDENTS = 1;
    public static final int TYPE_ROADWORKS = 2;

    public static final int PAGE_LIST = 0;
    public static final int PAGE_DETAIL = 1;
    public static final int PAGE_MAP = 2;

    public static final long ONE_HOUR = 60 * 60 * 1000;
    public static final long ONE_MINUTE = 60 * 1000;

    public static final String[] PLACE_NAMES = {"Glasgow (GCU)", "Edinburgh", "Aberdeen",
            "Dundee", "Inverness", "Perth", "Stirling", "Fort William", "Ayr", "Dumfries"};
    private static final double[] PLACE_LATITUDES = {55.8668, 55.9533, 57.1497,
            56.4620, 57.4778, 56.3950, 56.1165, 56.8198, 55.4586, 55.0709};
    private static final double[] PLACE_LONGITUDES = {-4.2500, -3.1883, -2.0943,
            -2.9707, -4.2247, -3.4308, -3.9369, -5.1052, -4.6292, -3.6051};
    public static final String[] DISTANCE_NAMES = {"Any distance", "3 miles", "5 miles",
            "10 miles", "25 miles", "50 miles"};
    private static final int[] DISTANCE_MILES = {0, 3, 5, 10, 25, 50};

    private final TrafficRepository repository;
    private ArrayList<Situation> allSituations = new ArrayList<>();

    private final MutableLiveData<ArrayList<Situation>> shownSituations;
    private final MutableLiveData<Situation> selectedSituation;
    private final MutableLiveData<String> statusMessage;
    private final MutableLiveData<Integer> loadProgress;

    private int typeFilter = TYPE_ALL;
    private String roadFilter = "";
    private int placeIndex = 0;
    private int distanceIndex = 0;
    private int currentPage = PAGE_LIST;
    private boolean started = false;

    private final Handler updateHandler;
    private long updateInterval = ONE_HOUR;

    public TrafficViewModel()
    {
        shownSituations = new MutableLiveData<>();
        shownSituations.setValue(new ArrayList<Situation>());
        selectedSituation = new MutableLiveData<>();
        statusMessage = new MutableLiveData<>();
        statusMessage.setValue("Starting...");
        loadProgress = new MutableLiveData<>();
        loadProgress.setValue(100);

        repository = new TrafficRepository();
        repository.setRepositoryListener(this);
        updateHandler = new Handler(Looper.getMainLooper());
    }

    public LiveData<ArrayList<Situation>> getShownSituations() {
        return shownSituations;
    }

    public LiveData<Situation> getSelectedSituation() {
        return selectedSituation;
    }

    public LiveData<String> getStatusMessage() {
        return statusMessage;
    }

    public LiveData<Integer> getLoadProgress() {
        return loadProgress;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public String getPlaceName() {
        return PLACE_NAMES[placeIndex];
    }

    // only kicks off the first load, ignored on screen rotations since the viewmodel stays alive
    public void start(String clientId, String clientKey, String storageFolder) {
        if (started) {
            return;
        }
        started = true;
        repository.setCredentials(clientId, clientKey);
        repository.setStorageFolder(storageFolder);
        refresh();
    }

    // triggers a fresh fetch from the repo and resets the auto-update timer
    public void refresh() {
        statusMessage.setValue("Getting data from Traffic Scotland...");
        loadProgress.setValue(0);
        repository.loadData();

        // cancel any pending timer first so manual refreshes restart the countdown
        updateHandler.removeCallbacks(updateTask);
        updateHandler.postDelayed(updateTask, updateInterval);
    }

    private final Runnable updateTask = new Runnable() {
        @Override
        public void run() {
            Log.d("ViewModel", "Automatic update");
            refresh();
        }
    };

    // switches auto-update frequency (hourly by default, or 1 min for demoing)
    public void setUpdateInterval(long milliseconds) {
        updateInterval = milliseconds;
        updateHandler.removeCallbacks(updateTask);
        updateHandler.postDelayed(updateTask, updateInterval);
    }

    @Override
    public void onLoadProgress(int percent) {
        loadProgress.setValue(percent);
    }

    @Override
    public void onDataReady(ArrayList<Situation> situations, String message) {
        allSituations = situations;
        statusMessage.setValue(message);
        loadProgress.setValue(100);
        applyFilters();

        // re-match the selected item by ID against the newly parsed list so details stay up to date
        Situation old = selectedSituation.getValue();
        if (old != null) {
            Situation replacement = null;
            for (Situation s : allSituations) {
                if (s.getId().equals(old.getId())) {
                    replacement = s;
                }
            }
            selectedSituation.setValue(replacement);
        }
    }

    @Override
    protected void onCleared() {
        updateHandler.removeCallbacks(updateTask);
        repository.setRepositoryListener(null);
        super.onCleared();
    }

    // updates active search filters; returns false if nothing changed (like when spinners re-fire on rotation)
    public boolean setFilters(int type, String road, int placeIndex, int distanceIndex) {
        if (type == typeFilter && road.equals(roadFilter)
                && placeIndex == this.placeIndex && distanceIndex == this.distanceIndex) {
            return false;
        }
        this.typeFilter = type;
        this.roadFilter = road;
        this.placeIndex = placeIndex;
        this.distanceIndex = distanceIndex;
        applyFilters();
        return true;
    }

    // filters all situations by type, road, and distance, then sorts the result
    private void applyFilters() {
        ArrayList<Situation> result = new ArrayList<>();
        int maxDistance = DISTANCE_MILES[distanceIndex];

        for (Situation s : allSituations) {
            if (typeFilter == TYPE_INCIDENTS && s.isRoadworks()) {
                continue;
            }
            if (typeFilter == TYPE_ROADWORKS && !s.isRoadworks()) {
                continue;
            }
            if (!s.matchesRoad(roadFilter)) {
                continue;
            }
            double distance = s.distanceTo(PLACE_LATITUDES[placeIndex], PLACE_LONGITUDES[placeIndex]);
            s.setDistanceMiles(distance);
            if (maxDistance > 0 && (distance < 0 || distance > maxDistance)) {
                continue;
            }
            result.add(s);
        }

        if (maxDistance > 0) {
            // closest first when filtering by distance
            Collections.sort(result, new Comparator<Situation>() {
                @Override
                public int compare(Situation a, Situation b) {
                    return Double.compare(a.getDistanceMiles(), b.getDistanceMiles());
                }
            });
        } else {
            // otherwise show incidents first, then roadworks sorted by worst delays
            Collections.sort(result, new Comparator<Situation>() {
                @Override
                public int compare(Situation a, Situation b) {
                    if (a.isRoadworks() != b.isRoadworks()) {
                        return a.isRoadworks() ? 1 : -1;
                    }
                    return b.getImpact().getDelayLevel() - a.getImpact().getDelayLevel();
                }
            });
        }
        Log.d("ViewModel", "Showing " + result.size() + " of " + allSituations.size());
        shownSituations.setValue(result);
    }

    public void selectSituation(Situation situation) {
        selectedSituation.setValue(situation);
    }
}
