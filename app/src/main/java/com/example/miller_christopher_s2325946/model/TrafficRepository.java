package com.example.miller_christopher_s2325946.model;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * fetches both XML feeds on a background thread, caches them locally for offline use, and posts results back to the UI thread
 */
public class TrafficRepository implements TrafficParser.ParserListener
{
    public interface RepositoryListener {
        void onLoadProgress(int percent);
        void onDataReady(ArrayList<Situation> situations, String statusMessage);
    }

    private static final String unplannedEventsURL =
            "https://datex2.trafficscotland.org/rest/2.3/publications/UnplannedEvents/Content.xml";

    private static final String roadworksURL =
            "https://datex2.trafficscotland.org/rest/2.3/publications/CurrentRoadworks/Content.xml";

    private static final String UNPLANNED_FILE = "unplanned.xml";
    private static final String ROADWORKS_FILE = "roadworks.xml";

    private static final int MESSAGE_PROGRESS = 1;
    private static final int MESSAGE_DATA_READY = 2;

    private String clientId = "";
    private String clientKey = "";
    private String storageFolder = "";

    private RepositoryListener listener;
    private final Executor executor;
    private final Handler uiHandler;
    private boolean loading = false;

    private String lastProblem = "";
    private int progressStart = 0;
    private int progressRange = 0;

    public TrafficRepository()
    {
        executor = Executors.newSingleThreadExecutor();

        uiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_DATA_READY) {
                    loading = false;
                }
                if (listener == null) {
                    return;
                }
                if (msg.what == MESSAGE_PROGRESS) {
                    listener.onLoadProgress((Integer) msg.obj);
                }
                else if (msg.what == MESSAGE_DATA_READY) {
                    LoadResult result = (LoadResult) msg.obj;
                    listener.onDataReady(result.situations, result.statusMessage);
                }
            }
        };
    }

    private static class LoadResult {
        ArrayList<Situation> situations;
        String statusMessage;
    }

    public void setRepositoryListener(RepositoryListener listener) {
        this.listener = listener;
    }

    public void setCredentials(String clientId, String clientKey) {
        this.clientId = clientId;
        this.clientKey = clientKey;
    }

    public void setStorageFolder(String storageFolder) {
        this.storageFolder = storageFolder;
    }

    // kicks off download + parse on a background thread; ignores duplicate calls while busy
    public void loadData() {
        if (loading) {
            Log.d("Repository", "Already loading, request ignored");
            return;
        }
        loading = true;
        executor.execute(loadTask);
    }

    // runs on the worker thread: fetches both feeds (or falls back to cached files), parses them, then posts to UI thread
    private final Runnable loadTask = new Runnable() {
        @Override
        public void run() {
            Log.d("Repository", "Worker thread started");
            sendProgress(2);

            String unplannedXml = downloadXml(unplannedEventsURL);
            String roadworksXml = null;
            if (unplannedXml != null) {
                sendProgress(10);
                roadworksXml = downloadXml(roadworksURL);
            }
            boolean liveData = (unplannedXml != null && roadworksXml != null);
            if (liveData) {
                saveToFile(UNPLANNED_FILE, unplannedXml);
                saveToFile(ROADWORKS_FILE, roadworksXml);
            } else {
                Log.e("Repository", "Download failed: " + lastProblem);
                unplannedXml = readFromFile(UNPLANNED_FILE);
                roadworksXml = readFromFile(ROADWORKS_FILE);
            }
            sendProgress(40);

            ArrayList<Situation> situations = new ArrayList<>();
            TrafficParser parser = new TrafficParser();
            parser.setParserListener(TrafficRepository.this);
            int incidents = 0;
            if (unplannedXml != null) {
                progressStart = 40;
                progressRange = 5;
                situations.addAll(parser.parseData(unplannedXml, false));
                incidents = situations.size();
            }
            if (roadworksXml != null) {
                progressStart = 45;
                progressRange = 55;
                situations.addAll(parser.parseData(roadworksXml, true));
            }
            int roadworks = situations.size() - incidents;

            String status;
            if (liveData) {
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.UK);
                status = "Live data, updated at " + timeFormat.format(new Date())
                        + ": " + incidents + " incidents, " + roadworks + " roadworks";
            } else if (unplannedXml != null || roadworksXml != null) {
                File saved = new File(storageFolder, ROADWORKS_FILE);
                SimpleDateFormat dateFormat = new SimpleDateFormat("EEE d MMM, HH:mm", Locale.UK);
                status = "OFFLINE (" + lastProblem + "). Showing data saved on "
                        + dateFormat.format(new Date(saved.lastModified()));
            } else {
                status = "No data (" + lastProblem + "). Check your connection and use Refresh.";
            }

            LoadResult result = new LoadResult();
            result.situations = situations;
            result.statusMessage = status;
            Message message = new Message();
            message.what = MESSAGE_DATA_READY;
            message.obj = result;
            uiHandler.sendMessage(message);
            Log.d("Repository", "Worker thread finished: " + status);
        }
    };

    private void sendProgress(int percent) {
        Message message = new Message();
        message.what = MESSAGE_PROGRESS;
        message.obj = percent;
        uiHandler.sendMessage(message);
    }

    // scales parser's 0-100% into the overall loading bar slice
    @Override
    public void onParsingProgress(int percent) {
        sendProgress(progressStart + (percent * progressRange) / 100);
    }

    // downloads an XML feed using basic auth, returns null on failure and sets lastProblem
    private String downloadXml(String address) {
        StringBuilder result = new StringBuilder();
        try {
            URL url = new URL(address);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(15000);
            urlConnection.setReadTimeout(30000);
            urlConnection.setRequestProperty("Authorization",
                    Base64.getEncoder().encodeToString((clientId + ":" + clientKey).getBytes()));
            try {
                int code = urlConnection.getResponseCode();
                if (code == HttpURLConnection.HTTP_UNAUTHORIZED
                        || code == HttpURLConnection.HTTP_FORBIDDEN) {
                    lastProblem = "access refused, check Client ID and Key";
                    return null;
                } else if (code != HttpURLConnection.HTTP_OK) {
                    lastProblem = "server error " + code;
                    return null;
                }
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(urlConnection.getInputStream(), "UTF-8"));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    result.append(inputLine).append("\n");
                }
                in.close();
            } finally {
                urlConnection.disconnect();
            }
        } catch (IOException e) {
            Log.e("Repository", "ioexception " + e);
            lastProblem = "no internet connection";
            return null;
        }

        // strip any garbage chars before the <? XML header
        int i = result.indexOf("<?");
        if (i < 0) {
            lastProblem = "unexpected reply from server";
            return null;
        }
        return result.substring(i);
    }

    // saves XML to internal storage for offline fallback
    private void saveToFile(String fileName, String data) {
        try {
            File f = new File(storageFolder, fileName);
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(data.getBytes("UTF-8"));
            fos.close();
        } catch (IOException e) {
            Log.e("Repository", "Could not save " + fileName);
        }
    }

    // reads back the last cached XML file, or null if none saved yet
    private String readFromFile(String fileName) {
        try {
            File f = new File(storageFolder, fileName);
            if (!f.exists()) {
                return null;
            }
            byte[] buffer = new byte[(int) f.length()];
            FileInputStream fin = new FileInputStream(f);
            int total = 0;
            while (total < buffer.length) {
                int count = fin.read(buffer, total, buffer.length - total);
                if (count < 0) {
                    break;
                }
                total += count;
            }
            fin.close();
            return new String(buffer, 0, total, "UTF-8");
        } catch (IOException e) {
            Log.e("Repository", "Could not read " + fileName);
            return null;
        }
    }
}
