package com.locsysrepo.sensors;

import com.locsysrepo.sensors.WiFiScan;

public interface OnWiFiDataCallback {
    public void onWiFiSample(WiFiScan scan);
}