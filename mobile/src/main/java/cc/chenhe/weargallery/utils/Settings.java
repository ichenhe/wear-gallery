package cc.chenhe.weargallery.utils;

import android.annotation.SuppressLint;
import android.content.Context;

/**
 * Created by 晨鹤 on 2016/12/12.
 */

public class Settings {
    public static final String NAME = "mainSettings";

    private static final String ITEM_WATCH_TOAST = "ITEM_WATCH_TOAST";//boolean
    private static final Boolean ITEM_WATCH_TOAST_D = true;
    private static final String ITEM_FORCE_TW_MODE = "ITEM_FORCE_TW_MODE"; // boolean
    private static final boolean ITEM_FORCE_TW_MODE_D = false; // boolean
    private static final String LAST_START_VERSION = "last_start_version";

    public static void lastStartVersion(Context context, int version) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(LAST_START_VERSION, version)
                .apply();
    }

    public static int lastStartVersion(Context context) {
        return context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
                .getInt(LAST_START_VERSION, 0);
    }

    @SuppressLint("ApplySharedPref")
    public static void forceTicMode(Context context, boolean force) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(ITEM_FORCE_TW_MODE, force)
                .commit();
    }

    public static boolean forceTicMode(Context context) {
        return context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
                .getBoolean(ITEM_FORCE_TW_MODE, ITEM_FORCE_TW_MODE_D);
    }

    public static void showWatchRequestToast(Context context, boolean show) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(ITEM_WATCH_TOAST, show)
                .apply();
    }

    public static boolean showWatchRequestToast(Context context) {
        return context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
                .getBoolean(ITEM_WATCH_TOAST, ITEM_WATCH_TOAST_D);
    }
}
