package cc.chenhe.weargallery.uilts;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.text.TextUtils;

public class WfSettings {
    private static final String NAME = "watchface";

    private static final String ITEM_IMAGE = "image";
    private static final String ITEM_SHOW_IN_AMBIENT = "show_in_ambient";
    private static final boolean ITEM_SHOW_IN_AMBIENT_D = false;
    private static final String ITEM_ANALOG_MODE = "analog_mode";
    private static final boolean ITEM_ANALOG_MODE_D = true;
    private static final String ITEM_SHOW_TIME = "show_time";
    private static final boolean ITEM_SHOW_TIME_D = true;
    private static final String ITEM_TIME_CONTENT = "time_content";
    private static final String ITEM_TIME_CONTENT_D = "HH:mm";
    private static final String ITEM_TIME_SIZE = "time_size";
    private static final float ITEM_TIME_SIZE_D = 36;
    private static final String ITEM_TIME_LEFT = "time_left";
    private static final int ITEM_TIME_LEFT_D = 100;
    private static final String ITEM_TIME_TOP = "time_right";
    private static final int ITEM_TIME_TOP_D = 100;
    private static final String ITEM_TIME_COLOR = "time_color";
    private static final String ITEM_TIME_COLOR_D = "#eeeeee";

    public static void setImage(Context context, String filePath) {
        if (filePath == null)
            context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
                    .edit()
                    .remove(ITEM_IMAGE)
                    .apply();
        else
            context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(ITEM_IMAGE, filePath)
                    .apply();
    }

    @Nullable
    public static String getImage(Context context) {
        return context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
                .getString(ITEM_IMAGE, null);
    }

    public static void showInAmbient(Context context, boolean show) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(ITEM_SHOW_IN_AMBIENT, show)
                .apply();
    }

    public static boolean showInAmbient(Context context) {
        return context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
                .getBoolean(ITEM_SHOW_IN_AMBIENT, ITEM_SHOW_IN_AMBIENT_D);
    }

    public static void analogMode(Context context, boolean analogMode) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(ITEM_ANALOG_MODE, analogMode)
                .apply();
    }

    public static boolean analogMode(Context context) {
        return context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
                .getBoolean(ITEM_ANALOG_MODE, ITEM_ANALOG_MODE_D);
    }

    public static void showTime(Context context, boolean show) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(ITEM_SHOW_TIME, show)
                .apply();
    }

    public static boolean showTime(Context context) {
        return context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
                .getBoolean(ITEM_SHOW_TIME, ITEM_SHOW_TIME_D);
    }

    public static void timeContent(Context context, String content) {
        SharedPreferences.Editor editor =
                context.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit();
        if (TextUtils.isEmpty(content))
            editor.putString(ITEM_TIME_CONTENT, ITEM_TIME_CONTENT_D);
        else {
            editor.putString(ITEM_TIME_CONTENT, content);
        }
        editor.apply();
    }

    public static String timeContent(Context context) {
        return context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
                .getString(ITEM_TIME_CONTENT, ITEM_TIME_CONTENT_D);
    }

    public static float timeSize(Context context) {
        return context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
                .getFloat(ITEM_TIME_SIZE, ITEM_TIME_SIZE_D);
    }

    public static void timeSize(Context context, float size) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
                .edit()
                .putFloat(ITEM_TIME_SIZE, size)
                .apply();
    }

    public static int timeLeft(Context context) {
        return context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
                .getInt(ITEM_TIME_LEFT, ITEM_TIME_LEFT_D);
    }

    public static void timeLeft(Context context, int left) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(ITEM_TIME_LEFT, left)
                .apply();
    }

    public static int timeTop(Context context) {
        return context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
                .getInt(ITEM_TIME_TOP, ITEM_TIME_TOP_D);
    }

    public static void timeTop(Context context, int top) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(ITEM_TIME_TOP, top)
                .apply();
    }

    public static String timeColor(Context context) {
        return context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
                .getString(ITEM_TIME_COLOR, ITEM_TIME_COLOR_D);
    }

    public static void timeColor(Context context, String color) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(ITEM_TIME_COLOR, color)
                .apply();
    }

}
