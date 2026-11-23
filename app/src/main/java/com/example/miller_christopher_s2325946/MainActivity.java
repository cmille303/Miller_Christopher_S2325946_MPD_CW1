// Name                 Christopher Miller
// Student ID           S2325946
// Programme of Study   Mobile Platform Development MHI326841-26-A-GLAS

package com.example.miller_christopher_s2325946;

import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.miller_christopher_s2325946.model.Situation;
import com.example.miller_christopher_s2325946.view.DetailFragment;
import com.example.miller_christopher_s2325946.view.SearchFragment;
import com.example.miller_christopher_s2325946.view.SituationListFragment;
import com.example.miller_christopher_s2325946.view.TrafficMapFragment;
import com.example.miller_christopher_s2325946.viewmodel.TrafficViewModel;

import java.util.ArrayList;

/**
 * main activity that hosts the fragments, toolbar, and viewflipper, and hooks them up to the viewmodel
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener,
        SearchFragment.SearchListener, SituationListFragment.ListListener,
        DetailFragment.DetailListener, TrafficMapFragment.MapListener
{
    private TrafficViewModel viewModel;

    private SituationListFragment listFragment;
    private DetailFragment detailFragment;

    private SearchFragment searchFragment;

    private TrafficMapFragment mapFragment;

    private TextView statusText;
    private ProgressBar progressBar;
    private ViewFlipper flipper;
    private Button listButton;
    private Button detailButton;
    private Button mapButton;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);

        statusText = (TextView) findViewById(R.id.statusText);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        flipper = (ViewFlipper) findViewById(R.id.flipper);
        listButton = (Button) findViewById(R.id.listButton);
        detailButton = (Button) findViewById(R.id.detailButton);
        mapButton = (Button) findViewById(R.id.mapButton);
        listButton.setOnClickListener(this);
        detailButton.setOnClickListener(this);
        mapButton.setOnClickListener(this);

        setUpFragments();

        viewModel = new ViewModelProvider(this).get(TrafficViewModel.class);
        observeViewModel();

        // restore whichever tab was open before rotation (defaults to list)
        showPage(viewModel.getCurrentPage());

        // hitting back goes to the list tab first instead of closing the app right away
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (viewModel.getCurrentPage() != TrafficViewModel.PAGE_LIST) {
                    showPage(TrafficViewModel.PAGE_LIST);
                } else {
                    finish();
                }
            }
        });

        viewModel.start(readKey("TS_CLIENT_ID"), readKey("TS_CLIENT_KEY"),
                getFilesDir().getAbsolutePath());
    }

    // puts each fragment into its container, or grabs the existing one if android recreated it on rotation
    private void setUpFragments() {
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();

        searchFragment = (SearchFragment) manager.findFragmentById(R.id.frameSearch);
        if (searchFragment == null) {
            searchFragment = SearchFragment.newInstance(TrafficViewModel.PLACE_NAMES,
                    TrafficViewModel.DISTANCE_NAMES);
            transaction.replace(R.id.frameSearch, searchFragment);
        }

        listFragment = (SituationListFragment) manager.findFragmentById(R.id.frameList);
        if (listFragment == null) {
            listFragment = new SituationListFragment();
            transaction.replace(R.id.frameList, listFragment);
        }
        detailFragment = (DetailFragment) manager.findFragmentById(R.id.frameDetail);
        if (detailFragment == null) {
            detailFragment = new DetailFragment();
            transaction.replace(R.id.frameDetail, detailFragment);
        }
        mapFragment = (TrafficMapFragment) manager.findFragmentById(R.id.frameMap);
        if (mapFragment == null) {
            mapFragment = new TrafficMapFragment();
            transaction.replace(R.id.frameMap, mapFragment);
        }
        transaction.commit();
    }

    private void observeViewModel() {
        viewModel.getShownSituations().observe(this, new Observer<ArrayList<Situation>>() {
            @Override
            public void onChanged(ArrayList<Situation> situations) {
                listFragment.showSituations(situations);
                mapFragment.showSituations(situations);
                detailFragment.showSituation(viewModel.getSelectedSituation().getValue(),
                        viewModel.getPlaceName());
            }
        });
        viewModel.getSelectedSituation().observe(this, new Observer<Situation>() {
            @Override
            public void onChanged(Situation situation) {
                detailFragment.showSituation(situation, viewModel.getPlaceName());
                listFragment.showSelected(situation);
                mapFragment.showSelected(situation);
            }
        });
        viewModel.getStatusMessage().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String message) {
                statusText.setText(message);
            }
        });
        viewModel.getLoadProgress().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer percent) {
                progressBar.setProgress(percent);
                if (percent < 100) {
                    progressBar.setVisibility(View.VISIBLE);
                } else {
                    progressBar.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        if (v == listButton) {
            showPage(TrafficViewModel.PAGE_LIST);
        } else if (v == detailButton) {
            showPage(TrafficViewModel.PAGE_DETAIL);
        } else if (v == mapButton) {
            showPage(TrafficViewModel.PAGE_MAP);
        }
    }

    // switches the viewflipper to the chosen tab and updates button colours
    private void showPage(int page) {
        viewModel.setCurrentPage(page);

        int frameId = R.id.frameList;
        if (page == TrafficViewModel.PAGE_DETAIL) {
            frameId = R.id.frameDetail;
        } else if (page == TrafficViewModel.PAGE_MAP) {
            frameId = R.id.frameMap;
        }
        // in landscape the list isn't inside the flipper so indexOfChild returns -1, fall back to first child
        int index = flipper.indexOfChild(findViewById(frameId));
        if (index < 0) {
            index = 0;
        }
        flipper.setDisplayedChild(index);

        int shownId = flipper.getChildAt(index).getId();
        highlightButton(listButton, shownId == R.id.frameList);
        highlightButton(detailButton, shownId == R.id.frameDetail);
        highlightButton(mapButton, shownId == R.id.frameMap);
    }

    private void highlightButton(Button button, boolean active) {
        if (active) {
            button.setBackgroundColor(getResources().getColor(R.color.tab_active, null));
            button.setTextColor(getResources().getColor(R.color.white, null));
        } else {
            button.setBackgroundColor(getResources().getColor(R.color.tab_inactive, null));
            button.setTextColor(getResources().getColor(R.color.text_dark, null));
        }
    }

    @Override
    public void onSituationSelected(Situation situation) {
        viewModel.selectSituation(situation);
        showPage(TrafficViewModel.PAGE_DETAIL);
    }

    @Override
    public void onSearchChanged(int type, String road, int placeIndex, int distanceIndex) {
        boolean changed = viewModel.setFilters(type, road, placeIndex, distanceIndex);
        if (changed) {
            int found = viewModel.getShownSituations().getValue().size();
            Toast.makeText(getApplicationContext(), found + " situations found",
                    Toast.LENGTH_SHORT).show();
            // jump back to list if user was viewing details so they see the new search results
            if (viewModel.getCurrentPage() == TrafficViewModel.PAGE_DETAIL) {
                showPage(TrafficViewModel.PAGE_LIST);
            }
        }
    }

    @Override
    public void onShowOnMap() {
        showPage(TrafficViewModel.PAGE_MAP);
    }

    @Override
    public void onMapSituationSelected(Situation situation) {
        viewModel.selectSituation(situation);
        showPage(TrafficViewModel.PAGE_DETAIL);
    }

    // reads API keys injected into the manifest from local.properties
    private String readKey(String name) {
        String key = "";
        try {
            Bundle metaData = getPackageManager().getApplicationInfo(getPackageName(),
                    PackageManager.GET_META_DATA).metaData;
            if (metaData != null) {
                Object value = metaData.get(getPackageName() + "." + name);
                if (value != null) {
                    key = String.valueOf(value);
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("MainActivity", "Could not read meta-data");
        }
        if (key.isEmpty()) {
            Log.e("MainActivity", "Key " + name + " not found, check local.properties");
        }
        return key;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menuRefresh) {
            viewModel.refresh();
        } else if (id == R.id.menuUpdateHour) {
            viewModel.setUpdateInterval(TrafficViewModel.ONE_HOUR);
            Toast.makeText(this, "Data will update every hour", Toast.LENGTH_LONG).show();
        } else if (id == R.id.menuUpdateMinute) {
            viewModel.setUpdateInterval(TrafficViewModel.ONE_MINUTE);
            Toast.makeText(this, "Demo mode: data will update every minute",
                    Toast.LENGTH_LONG).show();
        } else if (id == R.id.menuAbout) {
            showAboutDialog();
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void showAboutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.app_name);
        builder.setMessage(R.string.about_text);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }
}