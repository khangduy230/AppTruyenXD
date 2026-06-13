package com.nhom5.ftcomic.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

public class AppSettings {

    public static final String PREF_NAME = "AppSettingPrefs";

    public static final String KEY_NIGHT_MODE = "NightMode";
    public static final String KEY_FREQUENCY_POS = "frequency_pos";
    public static final String KEY_NETWORK_POS = "network_pos";
    public static final String KEY_AUTO_DELETE_POS = "auto_delete_pos";

    public static final int NETWORK_WIFI_AND_MOBILE = 0;
    public static final int NETWORK_ONLY_WIFI = 1;

    public static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isOnlyWifiDownload(Context context) {
        return prefs(context).getInt(KEY_NETWORK_POS, NETWORK_WIFI_AND_MOBILE) == NETWORK_ONLY_WIFI;
    }

    public static boolean canDownloadNow(Context context) {
        if (!isOnlyWifiDownload(context)) {
            return true;
        }

        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null) {
            return false;
        }

        Network network = cm.getActiveNetwork();

        if (network == null) {
            return false;
        }

        NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);

        return capabilities != null
                && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
    }

    public static int getAutoDeletePosition(Context context) {
        return prefs(context).getInt(KEY_AUTO_DELETE_POS, 0);
    }

    public static int getFrequencyPosition(Context context) {
        return prefs(context).getInt(KEY_FREQUENCY_POS, 0);
    }
}