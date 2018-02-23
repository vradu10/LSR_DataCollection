package com.locsysrepo.android;

import com.locsysrepo.components.Location;
import com.locsysrepo.components.Logger;
import com.locsysrepo.components.Point3D;
import com.locsysrepo.sensors.InertialSensorManager;
import com.locsysrepo.sensors.OnSensorDataCallback;
import com.locsysrepo.sensors.OnWiFiDataCallback;
import com.locsysrepo.sensors.SensorReading;
import com.locsysrepo.sensors.WiFiScan;
import com.locsysrepo.sensors.WiFiScanner;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by valentin
 */

/**
 * This service works in the background even when the map interface is closed or screen turned off.
 */

public class BackgroundService extends Service implements OnSensorDataCallback, OnWiFiDataCallback {

    public static final int     LOCATION_ESTIMATION_PERIOD = 2000;	// in milliseconds
    public static final String  LOCATION_BROADCAST_ACTION = "com.locmap.estimatedLocationBroadcast";

    private final static String backgroundLoggerPrefix = "background";

    public static boolean       isServiceRunning = false;
    public static boolean       isEstimatingLocation = false;

    private boolean  sensingAcc;
    private boolean  sensingMagn;
    private boolean  sensingGyro;
    private boolean  sensingWifi;

    public boolean   isLoggingSensorData = false;

    private final IBinder   mBinder = new BackgroundServiceBinder();

    private volatile Location  currentLocation;
    private Timer    locationEstimationTimer;
    private Logger   backgroundLogger;

    private InertialSensorManager ism;
    private WiFiScanner wiFiScanner;

    private SensorReading accReading, magnReading, gyroReading; // TODO consider here a list of samples
    private WiFiScan wiFiScan;


    @Override
    public void onCreate() {
        super.onCreate();

        ism = new InertialSensorManager(this);
        Log.i("serviceOnCreate","Created service");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Toast.makeText(this, "Service: on start command", Toast.LENGTH_SHORT).show();
        Log.i("serviceOnStartCommand","Service started");

        isServiceRunning = true;
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Toast.makeText(this, "Service: stopping", Toast.LENGTH_SHORT).show();

        enableLocationEstimation(false, null);
        enableSensorDataLogging(false, sensingAcc, sensingMagn, sensingGyro, sensingWifi);

        if (backgroundLogger != null)
            backgroundLogger.close();

        isServiceRunning = false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Toast.makeText(this, "Service: received bound", Toast.LENGTH_SHORT).show();
        Log.i("serviceIBinder","Bound to service");
        return mBinder;
    }

    /**
     *
     * Helper methods
     *
     */

    private void broadcastLocation() {
        Intent intent = new Intent(LOCATION_BROADCAST_ACTION);
        intent.putExtra("latitude", currentLocation.getPoint().getLat());
        intent.putExtra("longitude", currentLocation.getPoint().getLng());
        intent.putExtra("timestamp", currentLocation.getTimestamp());
        intent.putExtra("source", currentLocation.getSource());

        Log.i("broadcast", "location broadcasted:" + currentLocation.getPoint().getLat() + ", " + currentLocation.getPoint().getLng());

        sendBroadcast(intent);
        Log.i("Service broadcast loc", " sent location");
    }


    private void logData(String data) {
        checkBackgroundLoggerOn();
        backgroundLogger.writeLine(data);
    }



    private void checkBackgroundLoggerOn() {
        if (backgroundLogger == null) {
            backgroundLogger = new Logger(this, backgroundLoggerPrefix, "undefined");
        }
    }


    public class BackgroundServiceBinder extends Binder {
        BackgroundService getService() {
            Log.i("createBinder", "is service null? " + (BackgroundService.this == null));
            return BackgroundService.this;
        }
    }

    /**
     *
     *
     * Logging sensor data in background
     *
     */

    public void enableSensorDataLogging(boolean instructLogging,
                                        boolean sensingAcc,
                                        boolean sensingMagn,
                                        boolean sensingGyro,
                                        boolean sensingWifi) {

        if (isLoggingSensorData != instructLogging) {

            Toast.makeText(this, "Service: enable data collection - " +
                    instructLogging, Toast.LENGTH_SHORT).show();

            if (isLoggingSensorData) {  // turn off sensing
                if (this.sensingAcc)
                    ism.stopAccelerometerStream();
                if (this.sensingMagn)
                    ism.stopMagnetometerStream();
                if (this.sensingGyro)
                    ism.stopGyroscopeStream();
                if (this.sensingWifi)
                    wiFiScanner.stopScanning();
            } else {            // start streaming data
                if (sensingAcc)
                    ism.startAccelerometerStream(this);
                if (sensingMagn)
                    ism.startMagnetometerStream(this);
                if (sensingGyro)
                    ism.startGyroscopeStream(this);
                if (sensingWifi)
                    wiFiScanner = new WiFiScanner(this, true, this);
            }

            this.sensingAcc = sensingAcc;
            this.sensingMagn = sensingMagn;
            this.sensingGyro = sensingGyro;
            this.sensingWifi = sensingWifi;

            isLoggingSensorData = instructLogging;
        }

    }

    public boolean getIsLoggingSensorData() {
        return isLoggingSensorData;
    }

    @Override
    public void onSensorSample(SensorReading sample) {
        if (isLoggingSensorData) {
            logData(sample.toString());
        }

        if (sample.getType() == InertialSensorManager.SensorEnum.ACCELEROMETER)
            accReading = sample;
        else if (sample.getType() == InertialSensorManager.SensorEnum.MAGNETOMETER)
            magnReading = sample;
        else if (sample.getType() == InertialSensorManager.SensorEnum.GYROSCOPE)
            gyroReading = sample;
    }

    @Override
    public void onWiFiSample(WiFiScan scan) {
        if (isLoggingSensorData) {
            logData(scan.toString());
        }
        this.wiFiScan = scan;
    }


    /**
     *
     * Location estimation
     *
     */

    public void enableLocationEstimation(boolean instructEstimate, Location start) {

        if (isEstimatingLocation != instructEstimate) {
            Toast.makeText(this, "Service: enable location estim. - " +
                    instructEstimate, Toast.LENGTH_SHORT).show();

            if (instructEstimate) {
                // enable estimation

                userSetCurrentLocation(start);

                locationEstimationTimer = new Timer();
                locationEstimationTimer.scheduleAtFixedRate(new TimerTask() {
                    public void run() {
                        try {
                            Log.i("timer", "will do update at " + LOCATION_ESTIMATION_PERIOD + " millisec.");
                            updateCurrentLocation();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }, 300, LOCATION_ESTIMATION_PERIOD);

            } else {
                locationEstimationTimer.cancel();
            }
        }

        isEstimatingLocation = instructEstimate;

    }

    public boolean getIsEstimatingLocation() {
        return isEstimatingLocation;
    }

    public synchronized void userSetCurrentLocation(Location location) {
        currentLocation = location;

        checkBackgroundLoggerOn();
        logData(currentLocation.toString());

    }

    private void updateCurrentLocation() {
        currentLocation = estimateNextLocation(currentLocation);

        Log.i("Service.updateLocation", "Current location updated " +
                currentLocation.getPoint().getLat() + ", " + currentLocation.getPoint().getLng());
        broadcastLocation();

        logData(currentLocation.toString());
    }


    /**
     *
     * Intelligent location update here
     *
     * @param oldLocation
     * @return
     */

    private Location estimateNextLocation(Location oldLocation) {
        // TODO TASK: add intelligent update here by interpreting sensor data or send calls to cloud app

        Point3D newPoint = new Point3D(oldLocation.getPoint().getLat(), oldLocation.getPoint().getLng() + 0.00001); // simple update

        Location nextLocation = new Location(newPoint, System.currentTimeMillis(), Location.SOURCE_PDR_WIFI_ESTIMATION);

        return nextLocation;
    }

}