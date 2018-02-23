package com.locsysrepo.android;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.locsysrepo.components.Building;
import com.locsysrepo.components.Buildings_DB;
import com.locsysrepo.components.Location;
import com.locsysrepo.components.Logger;
import com.locsysrepo.components.Point3D;
import com.locsysrepo.sensors.InertialSensorManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.locsysrepo.sensors.OnSensorDataCallback;
import com.locsysrepo.sensors.OnWiFiDataCallback;
import com.locsysrepo.sensors.SensorReading;
import com.locsysrepo.sensors.WiFiScan;
import com.locsysrepo.sensors.WiFiScanner;

/**
 * Created by valentin
 */

public class InteractiveMapActivity extends FragmentActivity
        implements  OnMapReadyCallback,
                    OnMapLongClickListener,
                    OnMapClickListener,
                    OnMarkerClickListener,
                    OnSensorDataCallback,
                    OnWiFiDataCallback {

    protected final static LatLng   STARTING_LAT_LONG = new LatLng(55.944847, -3.187323);
    private final static int        STARTING_ZOOM = 16;
    private final static String[]   landmark_types = {"Corner", "Stairs", "Elevator", "Door", "Other"};
    private final static String[]   quickLongOptions = {"None", "Update location", "Scan at location"};
    private final static String     foregroundLoggerPrefix = "foreground";
    private final static int        SAMPLE_INTERVAL = 100;
    private final static boolean    aggregateSensorSample = true;

    private final static int QUICK_SET_NONE = 0;
    private final static int QUICK_SET_LOCATION = 1;
    private final static int QUICK_SET_LANDMARK = 2;


    private static boolean sensingAcc = true;
    private static boolean sensingMagn = true;
    private static boolean sensingGyro = true;
    private static boolean sensingWifi = true;

    private boolean isLoggingLocationInput   = false;
    private boolean isLoggingSensorsOnInput     = false;
    private boolean isLoggingSensorsContinuously    = false;
    private boolean isLocationEstimationRunning = false;

    private boolean isBoundToService          = false;

    private boolean     quickLongTap        = false;
    private int         quickLongTapOptionIndex = 0;
    private int         selectedLandmark    = 0;
    private Building    currentBuilding     = null;
    private boolean     buildingsEnabled    = true;
    private int         currentFloorIndex   = 6;
    private int         currentBuildingIndex = 1;
    private GroundOverlay floorMap          = null;
    private Logger      foregroundLogger    = null;


    private Map<Building, ArrayList<Location>[]> locations = null;
    private InertialSensorManager usm;

    /**
     * Android elements
     */

    private GoogleMap mMap = null;  // Might be null if Google Play services APK is not available.
    private ArrayList<Marker> markers = new ArrayList<Marker>();    // holds only the markers that are currently displayed.

    private Vibrator vibrator;
    private BackgroundService mService = null;

    private ImageButton optionsButton;
    private TextView topTextView;

    /**
     * Service related components
     */

    private BroadcastReceiver locationBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            double lat = intent.getDoubleExtra("latitude", 0);
            double lng = intent.getDoubleExtra("longitude", 0);
            long timestamp = intent.getLongExtra("timestamp", 0);
            byte source = intent.getByteExtra("source", Location.SOURCE_PDR_ESTIMATION);

            setCurrentLocation(lat, lng, timestamp, source);
        }
    };


    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {

            BackgroundService.BackgroundServiceBinder mBinder = (BackgroundService.BackgroundServiceBinder) binder;
            mService = mBinder.getService();

            isBoundToService = true;

            // TODO - everything goes in here when first interaction with service

            if (isLoggingSensorsContinuously) {
                mService.enableSensorDataLogging(isLoggingSensorsContinuously, sensingAcc, sensingMagn, sensingGyro, sensingWifi);
            }
            if (isLocationEstimationRunning) {
                Location startLoc;

                if (locations.get(currentBuilding)[currentFloorIndex].size() > 0)
                    startLoc = locations.get(currentBuilding)[currentFloorIndex].get(
                            locations.get(currentBuilding)[currentFloorIndex].size() - 1);
                else
                    startLoc = new Location(new Point3D(STARTING_LAT_LONG), System.currentTimeMillis(), (byte)10);
                mService.enableLocationEstimation(isLocationEstimationRunning, startLoc);
            }

            Log.i("mConnection", "Binded to service - mService is null? " + (mService == null));

        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;

            isBoundToService = false;

            Toast.makeText(InteractiveMapActivity.this, "Activity: UnBound from service", Toast.LENGTH_SHORT).show();
        }
    };


    /**
     *
     * Activity life cycle methods
     *
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_maps);
        setContentView(R.layout.activity_main);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        optionsButton = (ImageButton) findViewById(R.id.menuButton);
        optionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showOptionsDialog();
            }
        });

        topTextView = findViewById(R.id.textView_main);

        locations = new HashMap<Building, ArrayList<Location>[]>();

        usm = new InertialSensorManager(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //this.stopLocationService(); // this can be commented if you want your service to run in the background
        if (foregroundLogger != null)
            foregroundLogger.close();

        if (usm != null)
            usm.destroy();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (isBackgroundServiceRunning())
            bindToService();
        registerReceiver(locationBroadcastReceiver, new IntentFilter(BackgroundService.LOCATION_BROADCAST_ACTION));
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(locationBroadcastReceiver);
        this.unbindFromLocationService();

        Log.i("onPause", "took on pause");
    }




    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setIndoorEnabled(false);
        mMap.setBuildingsEnabled(false);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(STARTING_LAT_LONG, STARTING_ZOOM));

        mMap.setOnMapLongClickListener(this);
        mMap.setOnMapClickListener(this);
        mMap.setOnMarkerClickListener(this);

        if (buildingsEnabled)
            updateBuildingAndFloor();
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        vibrator.vibrate(60);
        if (quickLongTap) {

            if(quickLongTapOptionIndex == QUICK_SET_LOCATION)
                setCurrentLocation(latLng.latitude,
                    latLng.longitude,
                    System.currentTimeMillis(),
                    Location.SOURCE_USER_INPUT);
            else
                scanAtLocationOrLandmark(latLng.latitude, latLng.longitude, System.currentTimeMillis(), -1);

        } else
            this.showLongClickDialog(latLng);
    }

    @Override
    public void onMapClick(LatLng latLng) {
        Toast.makeText(this, "LatLng:\n" + latLng.latitude + ",\n" + latLng.longitude, Toast.LENGTH_SHORT).show();
        mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        marker.showInfoWindow();
        mMap.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));
        return true;
    }


    public boolean isBackgroundServiceRunning() {
        return BackgroundService.isServiceRunning;
    }

    private void startBackgroundService() {
        if (isBackgroundServiceRunning())
            return;

        Log.i("startservice", "activity starting Service");

        Location currentLocation;

        if (locations.get(currentBuilding)[currentFloorIndex].size() > 0)
            currentLocation = locations.get(currentBuilding)[currentFloorIndex].get(
                locations.get(currentBuilding)[currentFloorIndex].size() - 1);
        else
            currentLocation = new Location(new Point3D(STARTING_LAT_LONG), System.currentTimeMillis(), (byte)10);

        Intent serviceIntent = new Intent(this, BackgroundService.class);
        serviceIntent.putExtra("latitude", currentLocation.getPoint().getLat());
        serviceIntent.putExtra("longitude", currentLocation.getPoint().getLng());
        serviceIntent.putExtra("timestamp", currentLocation.getTimestamp());
        serviceIntent.putExtra("source", currentLocation.getSource());

        startService(serviceIntent);
    }

    private void stopBackgroundService() {
        if (isBoundToService)
            unbindFromLocationService();
        stopService(new Intent(this, BackgroundService.class));
        Log.i("service action", "sending stopping location action");
    }

    private void unbindFromLocationService() {
        if (mService != null) {
            unbindService(mConnection);
            mService = null;
            isBoundToService = false;
        }
    }

    private void bindToService() {
        if (!isBackgroundServiceRunning()) {
            startBackgroundService();
        }

        if (mService == null) {
            Intent intent = new Intent(this, BackgroundService.class);
            bindService(intent, mConnection, BIND_ABOVE_CLIENT);
        }

        Log.i("confirmBind", "is service null? " + (mService == null));
    }


    /**
     **
     **
     **  Interaction dialogs
     **
     **
     **/



    /**
     *
     *  Long Click Dialog
     *
     */
    public void showLongClickDialog(final LatLng latLng) {
        final long localTimestamp = System.currentTimeMillis();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Long Click");

        LayoutInflater inflater = getLayoutInflater();
        final View view = inflater.inflate(R.layout.long_click_dialog, null);

        final RadioButton currentLocationButton = (RadioButton) view.findViewById(R.id.radioButton_set_location);
        final RadioButton sampleHereButton = (RadioButton) view.findViewById(R.id.radioButton_sample_here);
        final RadioButton sampleLandmarkButton = (RadioButton) view.findViewById(R.id.radioButton_sample_landmark);
        Button undo = view.findViewById(R.id.undo_button);

        builder.setView(view).setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                if (currentLocationButton.isChecked()) {
                    setCurrentLocation(latLng.latitude, latLng.longitude, localTimestamp, Location.SOURCE_USER_INPUT);
                } else if (sampleHereButton.isChecked()) {
                    scanAtLocationOrLandmark(latLng.latitude, latLng.longitude, localTimestamp, -1);
                } else if (sampleLandmarkButton.isChecked()) {
                    scanAtLocationOrLandmark(latLng.latitude, latLng.longitude, localTimestamp, selectedLandmark);
                }
            }
        });

        // create the dialog frame
        final AlertDialog alertDialog = builder.show();

        undo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view2) {
                undoLastLocation();
                alertDialog.dismiss();
            }
        });

        ArrayList<String> spinnerArray = new ArrayList<String>();

        for (String l : landmark_types)
            spinnerArray.add(l);

        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                spinnerArray);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        Spinner spinner = (Spinner) view.findViewById(R.id.spinner_landmark);
        spinner.setAdapter(spinnerArrayAdapter);
        spinner.setSelection(selectedLandmark);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                //sampleLandmarkButton.setChecked(true);
                selectedLandmark = i;
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
    }



    /**
     *
     *  Options Dialog
     *
     */
    private void showOptionsDialog() {
        LayoutInflater inflater = getLayoutInflater();
        final View view = inflater.inflate(R.layout.options_dialog, null);

        if (isBoundToService) {
            isLoggingSensorsContinuously = mService.getIsLoggingSensorData();
            isLocationEstimationRunning = mService.getIsEstimatingLocation();
        }

        final int originalBuildingIndex = currentBuildingIndex;
        final int originalFloorIndex = currentFloorIndex;
        final int originalQuickLongTapOptionIndex = quickLongTapOptionIndex;

        final Context appContext = this;

        final CheckBox ckb_acc = (CheckBox) view.findViewById(R.id.checkBox_acc);
        final CheckBox ckb_magn = (CheckBox) view.findViewById(R.id.checkBox_magn);
        final CheckBox ckb_gyro = (CheckBox) view.findViewById(R.id.checkBox_gyro);
        final CheckBox ckb_wifi = (CheckBox) view.findViewById(R.id.checkBox_wifi);
        final CheckBox ckb_log_location_input = (CheckBox) view.findViewById(R.id.checkBox_log_location_input);
        final CheckBox ckb_log_sensor_input = (CheckBox) view.findViewById(R.id.checkBox_log_sensor_input);
        final CheckBox ckb_log_continuous = (CheckBox) view.findViewById(R.id.checkBox_log_continuous);
        final CheckBox ckb_estimation_running = (CheckBox) view.findViewById(R.id.checkBox_location_estimation);

        ckb_acc.setChecked(sensingAcc);
        ckb_magn.setChecked(sensingMagn);
        ckb_gyro.setChecked(sensingGyro);
        ckb_wifi.setChecked(sensingWifi);
        ckb_log_location_input.setChecked(isLoggingLocationInput);
        ckb_log_sensor_input.setChecked(isLoggingSensorsOnInput);
        ckb_log_continuous.setChecked(isLoggingSensorsContinuously);
        ckb_estimation_running.setChecked(isLocationEstimationRunning);

        ckb_log_sensor_input.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ckb_log_location_input.setChecked(true);
            }
        });


        AlertDialog.Builder builder = new AlertDialog.Builder(appContext);

        builder.setView(view).setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                if (currentBuildingIndex != originalBuildingIndex || currentFloorIndex != originalFloorIndex) {
                    updateBuildingAndFloor();
                    if (foregroundLogger != null) {
                        foregroundLogger = null;

                    }
                }

                sensingAcc  = ckb_acc.isChecked();
                sensingMagn = ckb_magn.isChecked();
                sensingGyro = ckb_gyro.isChecked();
                sensingWifi = ckb_wifi.isChecked();

                if (ckb_log_location_input.isChecked() != isLoggingLocationInput)
                    isLoggingLocationInput = ckb_log_location_input.isChecked();

                if (ckb_log_sensor_input.isChecked() != isLoggingSensorsOnInput)
                    isLoggingSensorsOnInput = ckb_log_sensor_input.isChecked();

                if (ckb_log_continuous.isChecked() != isLoggingSensorsContinuously) {
                    isLoggingSensorsContinuously = ckb_log_continuous.isChecked();
                    instructServiceContinuousSensing();
                }

                if (ckb_estimation_running.isChecked() != isLocationEstimationRunning) {
                    isLocationEstimationRunning = ckb_estimation_running.isChecked();

                    if (isLocationEstimationRunning)
                        startLocationEstimation();
                    else
                        stopLocationEstimation();
                }

                if (isBackgroundServiceRunning() && !isLoggingSensorsContinuously && !isLocationEstimationRunning)
                    stopBackgroundService();

                if (originalQuickLongTapOptionIndex != quickLongTapOptionIndex) {
                    if (quickLongTapOptionIndex == QUICK_SET_NONE)
                        quickLongTap = false;
                    else
                        quickLongTap = true;
                }


            }
        });

        if (buildingsEnabled) {
            final Spinner spinner_building = (Spinner) view.findViewById(R.id.spinner_building);
            final Spinner spinner_floor = (Spinner) view.findViewById(R.id.spinner_floor);

            // set options for the building spinner
            ArrayList<String> spinnerArray_building = new ArrayList<String>();

            for (String name : Buildings_DB.buildingNames)
                spinnerArray_building.add(name);

            ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(
                    appContext,
                    android.R.layout.simple_spinner_item,
                    spinnerArray_building);

            spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner_building.setAdapter(spinnerArrayAdapter);
            spinner_building.setSelection(currentBuildingIndex);

            // define behaviour for building spinner
            spinner_building.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    currentBuildingIndex = i;

                    // update the floor spinner
                    ArrayList<String> spinnerArray_floor = new ArrayList<String>();

                    for (String floor : Buildings_DB.getBuilding(i).getFloors())
                        spinnerArray_floor.add(floor);

                    ArrayAdapter<String> spinnerArrayAdapter_floor = new ArrayAdapter<String>(
                            appContext,
                            android.R.layout.simple_spinner_item,
                            spinnerArray_floor);

                    spinnerArrayAdapter_floor.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                    spinner_floor.setAdapter(spinnerArrayAdapter_floor);
                    spinner_floor.setSelection(
                            (currentFloorIndex < Buildings_DB.getBuilding(i).getFloors().length)?
                                currentFloorIndex : Buildings_DB.getBuilding(i).getFloors().length - 1
                    );
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            });

            // Behaviour for the floors spinner
            spinner_floor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    currentFloorIndex = i;
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            });
        }

        // Quick Long Tap
        final Spinner spinner_quick_long = (Spinner) view.findViewById(R.id.spinner_quick_long);
        ArrayList<String> spinnerArray_quickLong = new ArrayList<String>();
        for (String option : quickLongOptions)
            spinnerArray_quickLong.add(option);
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(
                appContext,
                android.R.layout.simple_spinner_item,
                spinnerArray_quickLong);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_quick_long.setAdapter(spinnerArrayAdapter);
        spinner_quick_long.setSelection(quickLongTapOptionIndex);

        // define behaviour for building spinner
        spinner_quick_long.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                quickLongTapOptionIndex = i;
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        builder.show();
    }

    /**
     *
     *
     * Actions
     *
     *
     */

    private void updateBuildingAndFloor() {

        currentBuilding = Buildings_DB.getBuilding(currentBuildingIndex);

        if (floorMap != null)
            floorMap.remove();

        int drawableId = currentBuilding.getDrawableId(currentFloorIndex);
        Log.i("main", "Drawable id: " + drawableId);
        if (drawableId != -100) {

            floorMap = mMap.addGroundOverlay(new GroundOverlayOptions()
                    .image(BitmapDescriptorFactory.fromResource(drawableId))
                    .position(currentBuilding.getCenter(), currentBuilding.getWidth())
            );

        }

        for (Marker m : markers)
            m.remove();
        markers.clear();

        // update points
        if (!locations.containsKey(currentBuilding)) {
            locations.put(currentBuilding, new ArrayList[currentBuilding.getNumberOfFloors()]);
        }
        if (locations.get(currentBuilding)[currentFloorIndex] == null)
            locations.get(currentBuilding)[currentFloorIndex] = new ArrayList<Location>();


        for (Location l : locations.get(currentBuilding)[currentFloorIndex])
                drawPoint(l.getLatLng(), l.getTimestamp());

        topTextView.setText("Building: " + currentBuilding.name + ";\tFloor: " + currentBuilding.getFloors()[currentFloorIndex]);

    }

    private void setCurrentLocation(double lat, double lng, long timestamp, byte source) {
        LatLng latLng = new LatLng(lat, lng);
        Location location = new Location(new Point3D(latLng), timestamp, source);

        Log.i("setCurrentLoc", "setting currentLoc: " + latLng);


        locations.get(currentBuilding)[currentFloorIndex].add(location);

        drawPoint(latLng, timestamp);

        if (source == Location.SOURCE_USER_INPUT) {

            if (isLoggingLocationInput)
                logData(location.toString());

            if (isBoundToService) {
                mService.userSetCurrentLocation(location);
            }
        }
    }

    private void undoLastLocation() {

        if (! locations.get(currentBuilding)[currentFloorIndex].isEmpty()) {
            Location lastLoc = locations.get(currentBuilding)[currentFloorIndex].get(
                    locations.get(currentBuilding)[currentFloorIndex].size() - 1);

            locations.get(currentBuilding)[currentFloorIndex].remove(lastLoc);

            logData("<undo_loc time=\"" + lastLoc.getTimestamp() +
                    "\" lat=\"" + lastLoc.getLatLng().latitude +
                    "\" lng=\"" + lastLoc.getLatLng().longitude + "\" />");

            updateBuildingAndFloor();
        }
    }

    private void scanAtLocationOrLandmark(double lat, double lng, long timestamp, int landmarkIndex) {
        /**
         * simple location has landmarkIndex = -1
         * Corner   = 0
         * Stairs   = 1
         * Elevator = 2
         * Door     = 3
         * Other    = 4
         */

        if (isLoggingLocationInput) {
            checkForegroundLoggerOn();

            LatLng latLng = new LatLng(lat, lng);
            Log.i("setCurrentLoc", "setting currentLoc: " + latLng);

            logData("<loc time=\"" + timestamp +
                    "\" lat=\"" + latLng.latitude +
                    "\" lng=\"" + latLng.longitude +
                    "\" lmk=\"" + landmarkIndex + "\" />");
        }

        if (isLoggingSensorsOnInput) {
            checkForegroundLoggerOn();

            if (sensingAcc)
                usm.sampleAccelerometer(this, aggregateSensorSample, SAMPLE_INTERVAL);
            if (sensingMagn)
                usm.sampleMagnetometer(this, aggregateSensorSample, SAMPLE_INTERVAL);
            if (sensingGyro)
                usm.sampleGyroscope(this, aggregateSensorSample, SAMPLE_INTERVAL);
            if (sensingWifi)
                new WiFiScanner(this, false, this);
        }

        drawPoint(new LatLng(lat, lng), timestamp);
    }

    private void instructServiceContinuousSensing() {
        if (!isBoundToService)
            bindToService();
        else
            mService.enableSensorDataLogging(isLoggingSensorsContinuously, sensingAcc, sensingMagn, sensingGyro, sensingWifi);
    }

    private void startLocationEstimation() {
        if (isBoundToService) {


            Log.i("activity", "mservice is null? (on start location estimation) " + (mService == null));

            Location start;
            if (locations.get(currentBuilding)[currentFloorIndex].size() > 0)
                start = locations.get(currentBuilding)[currentFloorIndex].get(
                        locations.get(currentBuilding)[currentFloorIndex].size() - 1);
            else
                start = new Location(new Point3D(STARTING_LAT_LONG), System.currentTimeMillis(), (byte) 10);

            mService.enableLocationEstimation(true, start);
        } else
            bindToService();
    }

    private void stopLocationEstimation() {
        if (isBoundToService) {
            mService.enableLocationEstimation(false, null);
        } else
            bindToService();

    }

    private void logData(String data) {
        checkForegroundLoggerOn();
        foregroundLogger.writeLine(data);
    }

    private void checkForegroundLoggerOn() {
        if (foregroundLogger == null) {
            foregroundLogger = new Logger(this, foregroundLoggerPrefix, currentBuilding.name);
        }
    }


    /*
     *
     * Helper methods
     *
     */

    private void drawPoint(LatLng latLng, long timestamp) {
        // check if previous point needs to be recolored
        if (markers.size() > 0) {
            Marker prevMarker = markers.get(markers.size() - 1);
            prevMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.small_red));
        }

        Marker marker = mMap.addMarker(new MarkerOptions().position(latLng).anchor(0.5f, 0.5f)
                .title("Point " + markers.size()).snippet("" + latLng + " @" + timestamp)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.small_green)));

        markers.add(marker);

        if (markers.size() > 1) {
            float[] res = new float[1];
            Marker m1 = markers.get(markers.size() - 1);
            Marker m2 = markers.get(markers.size() - 2);
            android.location.Location.distanceBetween(m1.getPosition().latitude, m1.getPosition().longitude,
                    m2.getPosition().latitude, m2.getPosition().longitude, res);
            topTextView.setText("Distance:" + res[0]);
        }

    }

    @Override
    public void onSensorSample(SensorReading sample) {
        logData(sample.toString());
    }

    @Override
    public void onWiFiSample(WiFiScan scan) {
        logData(scan.toString());
    }
}
