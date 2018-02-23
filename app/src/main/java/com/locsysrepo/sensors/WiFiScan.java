package com.locsysrepo.sensors;

import android.net.wifi.ScanResult;

import com.locsysrepo.utils.WiFiChannel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by valentin
 */

public class WiFiScan {

    public class WiFiRecord {
        String bssid;
        int signal;
        int channel;

        public WiFiRecord(String bssid, int signal, int channel) {
            this.bssid = bssid;
            this.signal = signal;
            this.channel = channel;
        }
    }

    private List<WiFiRecord> scan;
    private long timestamp;

    public WiFiScan(List<ScanResult> results) {
        scan = new ArrayList<WiFiRecord>();

        for (ScanResult result : results) {
            scan.add(new WiFiRecord(result.BSSID,
                                    result.level,
                                    WiFiChannel.getChannel(result.frequency)));
        }

        this.timestamp = System.currentTimeMillis();

    }

    public String toString() {
        StringBuffer message = new StringBuffer();
        message.append("<wr t=\"");
        message.append(timestamp);
        message.append("\">\n");

        for (WiFiRecord record : scan) {
            message.append("<r b=\"");
            message.append(record.bssid);
            message.append("\" s=\"");
            message.append(record.signal);
            message.append("\" c=\"");
            message.append(record.channel);
            message.append("\" />\n");
        }
        message.append("</wr>");

        return message.toString();
    }
}