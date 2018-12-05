package cc.chenhe.weargallery.uilts;

import android.content.Context;

/**
 * Created by 宸赫 on 2016/2/21.
 */
public class Settings {
    public static final String NAME = "mainSettings";

    public static final String ITEM_WATCH_WEAK_LOCK = "ITEM_WATCH_WEAK_LOCK";//boolean
    public static final boolean ITEM_WATCH_WEAK_LOCK_D = true;
    public static final String ITEM_MAX_BRIGHTNESS = "ITEM_MAX_BRIGHTNESS";//boolean
    public static final boolean ITEM_MAX_BRIGHTNESS_D = false;

    private static final String TIP_SWITCH_WATCHFACE = "tip_switch_watchface";
    private static final String TIP_ALWAYS_ON = "tip_always_on";
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

    public static void showSwitchWfTip(Context context, boolean show) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(TIP_SWITCH_WATCHFACE, show)
                .apply();
    }

    public static boolean showSwitchWfTip(Context context) {
        return context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
                .getBoolean(TIP_SWITCH_WATCHFACE, true);
    }

    public static void showAlwaysOnTip(Context context, boolean show) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(TIP_ALWAYS_ON, show)
                .apply();
    }

    public static boolean showAlwaysOnTip(Context context) {
        return context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
                .getBoolean(TIP_ALWAYS_ON, true);
    }
}
