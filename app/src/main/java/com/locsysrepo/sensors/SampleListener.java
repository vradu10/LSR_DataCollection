package com.locsysrepo.sensors;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.widget.Toast;

import com.locsysrepo.components.Logger;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by valentin
 */

public class SampleListener implements SensorEventListener {

    public final long INTERVAL = 100;    // default in milliseconds

    private long interval;
    private boolean aggregate = true;
    private long startTime = -1;
    private ArrayList<Float>[] records;
    private ArrayList<Long> times;
    private Context context;
    private InertialSensorManager.SensorEnum sensor;
    private OnSensorDataCallback sensorDataCallback;

    public SampleListener(InertialSensorManager.SensorEnum sensor,
                          OnSensorDataCallback sensorDataCallback,
                          boolean aggregate, long interval, Context context) {
        this.sensor = sensor;
        this.aggregate = aggregate;
        this.interval = interval;
        this.context = context;
        this.sensorDataCallback = sensorDataCallback;

        if (aggregate) {
            records = new ArrayList[3];
            records[0] = new ArrayList<Float>();
            records[1] = new ArrayList<Float>();
            records[2] = new ArrayList<Float>();
            times = new ArrayList<Long>();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long time = event.timestamp;
        if (this.startTime < 0)
            this.startTime = time;

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        boolean stillTime = time - this.startTime < this.interval;

        if (stillTime) {
            if (aggregate) {
                records[0].add(x);
                records[1].add(y);
                records[2].add(z);
                times.add(time);

                return;
            }

            // else going forward to writing data to logger

        } else {
            stopListening();

            if (aggregate) {

                Collections.sort(records[0]);
                Collections.sort(records[1]);
                Collections.sort(records[2]);

                x = records[0].get(records[0].size() / 2);
                y = records[1].get(records[1].size() / 2);
                z = records[2].get(records[2].size() / 2);
                time = times.get(times.size() / 2);
            } else
                // do write to file if time has exceeded
                return;
        }

        sensorDataCallback.onSensorSample(new SensorReading(x, y, z, time, sensor, -1));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}

    private void stopListening() {
        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensorManager.unregisterListener(this);
        Toast.makeText(context, "Sensor sample collected!", Toast.LENGTH_SHORT).show();
    }
}
