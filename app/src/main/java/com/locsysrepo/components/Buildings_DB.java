package com.locsysrepo.components;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.locsysrepo.android.R;

/**
 * Created by valentin
 */

public class Buildings_DB {

    /**
     * Nasty hardcoding and redundancy in here, names, floor names, etc; careful with updates
     */

    public static String[] buildingNames = {"Informatics Forum", "Appleton Tower", "Main Library"};
    public static Building[] buildings = new Building[buildingNames.length];

    public static Building getBuilding(int index) {
        if (index == 0) {
            if (buildings[index] != null)
                return buildings[index];

            buildings[index] = new Building("Informatics Forum", new String[]{"-1", "0", "1", "2", "3", "4", "5"});
            buildings[index].setDrawables(new int[]{
                    R.drawable.b1,
                    R.drawable.f0,
                    R.drawable.f1,
                    R.drawable.f2,
                    R.drawable.f3,
                    R.drawable.f4,
                    R.drawable.f5,
            });
            buildings[index].setCenterAndWidth(new LatLng(55.9448695, -3.1871785), 80f);
        }

        if (index == 1) {
            if (buildings[index] != null)
                return buildings[index];
            Log.i("Building", "make building");
            buildings[index] = new Building("Appleton Tower", new String[]{"-1", "0", "1", "2", "3", "4", "5", "6", "7", "8"});
            buildings[index].setDrawables(new int[]{
                    R.drawable.at_null,
                    R.drawable.at_null,
                    R.drawable.at_null,
                    R.drawable.at_null,
                    R.drawable.at_null,
                    R.drawable.at_null,
                    R.drawable.at_floor5_v4,
                    R.drawable.at_null,
                    R.drawable.at_null,
                    R.drawable.at_null,
            });
            buildings[index].setCenterAndWidth(new LatLng(55.94443, -3.18675), 52f);
        }

        if (index == 2) {
            if (buildings[index] != null)
                return buildings[index];
            buildings[index] = new Building("Main Library", new String[]{"0", "1", "2", "3", "4", "5"});
        }
        return buildings[index];

    }

}