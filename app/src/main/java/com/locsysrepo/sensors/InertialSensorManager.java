package com.locsysrepo.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.widget.Toast;

import com.locsysrepo.components.Logger;

/**
 * Created by valentin
 */

public class InertialSensorManager {
    /**
     * Two operation modes:
     *  1. Streaming
     *  2. One sensor sample (aggregate over a very short period of time) or short interval data collection
     *
     *  Designed to accept different loggers for multiple tasks performing at the same time.
     */

    private static final int ACCELEROMETER_SAMPLING = SensorManager.SENSOR_DELAY_FASTEST;
    private static final int MAGNETIC_SAMPLING = SensorManager.SENSOR_DELAY_FASTEST;
    private static final int GYROSCOPE_SAMPLING = SensorManager.SENSOR_DELAY_FASTEST;

    public enum SensorEnum {
        ACCELEROMETER(0, Sensor.TYPE_ACCELEROMETER, 'a', ACCELEROMETER_SAMPLING),
        MAGNETOMETER(1, Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED, 'm', MAGNETIC_SAMPLING),
        GYROSCOPE(2, Sensor.TYPE_GYROSCOPE_UNCALIBRATED, 'g', GYROSCOPE_SAMPLING);

        private final int order;
        private final int type;
        private final char tag;
        private int sampling;
        private SensorEnum(int order, int type, char tag, int samplingRate) {
            this.order = order;
            this.type = type;
            this.tag = tag;
            this.sampling = samplingRate;
        }
        public int getOrder() {
            return this.order;
        }
        public int getSensorType() {
            return this.type;
        }
        public char getTag() {
            return  this.tag;
        }
        public int getSamplingRate() { return this.sampling; }
    }

    private Object[]    streamListener = new Object[SensorEnum.values().length];
    private long        sampleFinishTimestamp = System.currentTimeMillis();

    private SensorManager sensorManager;
    private Context context;

    public InertialSensorManager(Context context) {
        this.context = context;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    }

    /** For the Accelerometer sensor **/

    public void startAccelerometerStream(OnSensorDataCallback sensorDataCallback) {
        this.startSensorStream(SensorEnum.ACCELEROMETER, sensorDataCallback);
    }
    public void stopAccelerometerStream() {
        this.stopSensorStream(SensorEnum.ACCELEROMETER);
    }
    public void sampleAccelerometer (OnSensorDataCallback sensorDataCallback,
                                     boolean aggregate, long interval) {
        this.sampleSensor(SensorEnum.ACCELEROMETER, sensorDataCallback, aggregate, interval);
    }


    /** For the Magnetometer sensor **/

    public void startMagnetometerStream(OnSensorDataCallback sensorDataCallback) {
        this.startSensorStream(SensorEnum.MAGNETOMETER, sensorDataCallback);
    }
    public void stopMagnetometerStream() {
        this.stopSensorStream(SensorEnum.MAGNETOMETER);
    }
    public void sampleMagnetometer (OnSensorDataCallback sensorDataCallback,
                                    boolean aggregate, long interval) {
        this.sampleSensor(SensorEnum.MAGNETOMETER, sensorDataCallback, aggregate, interval);
    }

    /** For the Gyroscope sensor **/

    public void startGyroscopeStream(OnSensorDataCallback sensorDataCallback) {
        this.startSensorStream(SensorEnum.GYROSCOPE, sensorDataCallback);
    }
    public void stopGyroscopeStream() {
        this.stopSensorStream(SensorEnum.GYROSCOPE);
    }
    public void sampleGyroscope (OnSensorDataCallback sensorDataCallback,
                                 boolean aggregate, long interval) {
        this.sampleSensor(SensorEnum.GYROSCOPE, sensorDataCallback, aggregate, interval);
    }


    /** For all the sensors at once **/

    public void startAllSensorsStream(OnSensorDataCallback sensorDataCallback) {
        this.startSensorStream(SensorEnum.ACCELEROMETER, sensorDataCallback);
        this.startSensorStream(SensorEnum.MAGNETOMETER, sensorDataCallback);
        this.startSensorStream(SensorEnum.GYROSCOPE, sensorDataCallback);
    }
    public void stopAllSensorsStream() {
        this.stopSensorStream(SensorEnum.ACCELEROMETER);
        this.stopSensorStream(SensorEnum.MAGNETOMETER);
        this.stopSensorStream(SensorEnum.GYROSCOPE);
    }
    public void sampleFromAllSensors (OnSensorDataCallback sensorDataCallback,
                                      boolean aggregate, long interval) {
        this.sampleSensor(SensorEnum.ACCELEROMETER, sensorDataCallback, aggregate, interval);
        this.sampleSensor(SensorEnum.MAGNETOMETER, sensorDataCallback, aggregate, interval);
        this.sampleSensor(SensorEnum.GYROSCOPE, sensorDataCallback, aggregate, interval);
    }

    /**
     * Make sure that all sensing is turned off
     */
    public void destroy() {
        // stop all streaming sensors
        for (SensorEnum sensor : SensorEnum.values()) {
            if (streamListener[sensor.getOrder()] != null) {
                this.stopSensorStream(sensor);
            }
        }

        long waitForSampleFinish = sampleFinishTimestamp - System.currentTimeMillis();
        if (waitForSampleFinish > 0)
            try {
                Thread.sleep(waitForSampleFinish);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
    }

    private void startSensorStream(SensorEnum sensor, OnSensorDataCallback sensorDataCallback) {
        if (this.streamListener[sensor.getOrder()] != null)
            return;

        StreamListener listener = new StreamListener(sensor, sensorDataCallback);

        boolean sensorAvailable = sensorManager.registerListener(listener,
                sensorManager.getDefaultSensor(sensor.getSensorType()),
                sensor.getSamplingRate());

        if (!sensorAvailable)
            Log.e("InertialSensorManager", "In start sensor stream: Phone doesn't have sensor " + sensor);

        this.streamListener[sensor.getOrder()] = listener;
    }

    private void stopSensorStream(SensorEnum sensor) {
        if (this.streamListener[sensor.getOrder()] == null)
            return;

        sensorManager.unregisterListener((StreamListener)streamListener[sensor.getOrder()]);
        this.streamListener[sensor.getOrder()] = null;
    }

    private void sampleSensor(SensorEnum sensor, OnSensorDataCallback sensorDataCallback,
                              boolean aggregate, long interval) {

        SampleListener listener = new SampleListener(sensor, sensorDataCallback, aggregate, interval, context);

        boolean sensorAvailable = sensorManager.registerListener(listener,
                sensorManager.getDefaultSensor(sensor.getSensorType()),
                SensorManager.SENSOR_DELAY_FASTEST);

        sampleFinishTimestamp = (System.currentTimeMillis() + interval > sampleFinishTimestamp)?
                System.currentTimeMillis() + interval : sampleFinishTimestamp;

        if (!sensorAvailable) {
            Toast.makeText(context, "Phone doesn't have sensor " + sensor, Toast.LENGTH_SHORT).show();
        }
    }
}
