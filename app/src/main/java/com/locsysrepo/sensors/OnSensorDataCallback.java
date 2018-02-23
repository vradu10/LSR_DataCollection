package com.locsysrepo.sensors;

public interface OnSensorDataCallback {
    public void onSensorSample(SensorReading sample);
}