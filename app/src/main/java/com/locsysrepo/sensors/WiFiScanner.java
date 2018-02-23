package com.locsysrepo.sensors;

import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.Toast;

import com.locsysrepo.components.Logger;

/**
 * Created by valentin
 */

public class WiFiScanner extends BroadcastReceiver {

    private Context context;
    private WifiManager wifiManager;
    private boolean repeated;
    private OnWiFiDataCallback wiFiDataCallback;

    public WiFiScanner(Context context, boolean repeated, OnWiFiDataCallback wiFiDataCallback) {
        super();
        this.context = context;
        this.repeated = repeated;
        this.wiFiDataCallback = wiFiDataCallback;

        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(context, "WiFi not enabled", Toast.LENGTH_SHORT).show();
            return;
        }

        context.registerReceiver(this, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();
    }

    @Override
    public void onReceive(Context c, Intent intent) {

        List<ScanResult> results = wifiManager.getScanResults();

        WiFiScan scan = new WiFiScan(results);

        // push data to receiver
        wiFiDataCallback.onWiFiSample(scan);

        if (repeated)
            wifiManager.startScan();
        else
            stopScanning();
    }

    public void stopScanning() {
        context.unregisterReceiver(this);
    }

}
