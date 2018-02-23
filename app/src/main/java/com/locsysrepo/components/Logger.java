package com.locsysrepo.components;

import android.content.Context;
import android.os.Environment;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.sql.Timestamp;

/**
 * Created by valentin
 */

public class Logger {

    private final static String LOGS_FOLDER = "localization";

    private BufferedWriter bw;
    private Context context;
    private String buildingName;

    public Logger(Context context, String prefix, String buildingName) {
        this.context = context;
        this.buildingName = buildingName;
        open(prefix);
    }

    public synchronized void write(String data) {
        try {
            Log.i("logger write", data);
            bw.append(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void writeLine(String data) {
        try {
            bw.append(data).append("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state))
            return true;
        else
            return false;
    }

    private void open(String prefix) {
        if (!isExternalStorageWritable()) {
            Toast.makeText(context, "Check if memory card is inserted", Toast.LENGTH_LONG).show();
            return;
        }

        if (bw == null) {
            File dir = new File (Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + LOGS_FOLDER);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            Date date = new Date();
            Timestamp timestamp = new Timestamp(date.getTime());
            String filename = timestamp.toString();

            File file = new File(dir, prefix + "_" + filename.substring(0, filename.length() - 4).replaceAll(":", "-") + ".xml");

            FileWriter fw = null;
            try {
                fw = new FileWriter(file.getAbsoluteFile());
            } catch (IOException e) {
                e.printStackTrace();
            }
            bw = new BufferedWriter(fw);

            write("<data phone=\"" +
                    Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID) +
                    "\" building=\"" + buildingName + "\" t=\"" + timestamp.toString() + "\">\n");

        }

    }

    public void close() {
        if (bw != null) {
            this.write("</data>");
            try {
                this.bw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            bw = null;
        }
    }

}
