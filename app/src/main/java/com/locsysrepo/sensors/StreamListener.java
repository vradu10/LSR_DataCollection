package com.locsysrepo.sensors;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

/**
 * Created by valentin
 */

public class StreamListener implements SensorEventListener {

    private InertialSensorManager.SensorEnum type;
    private OnSensorDataCallback sensorDataCallback;

    public StreamListener(InertialSensorManager.SensorEnum type, OnSensorDataCallback sensorDataCallback) {
        this.type = type;
        this.sensorDataCallback = sensorDataCallback;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        SensorReading sr = new SensorReading(event.values[0], event.values[1], event.values[2], System.currentTimeMillis(), type, event.timestamp);

        sensorDataCallback.onSensorSample(sr);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}
}
