package cc.chenhe.weargallery.uilts;

import android.app.Activity;
import android.content.Context;
import android.view.WindowManager;

import java.io.File;

import cc.chenhe.weargallery.common.CUtils;

/**
 * Created by 晨鹤 on 2016/12/15.
 */

public class Utils {

    // 图片双击放大的倍数。1表示放大1倍，即原图的2倍。
    public static final int IMAGE_VIEW_MAX_DPI = 160;
    public static final int IMAGE_VIEW_MID_DPI = 600;

    /*目录缩略图缓存路径*/
    public static String Path_cache_gallery;
    /*图片缩略图缓存路径*/
    public static String Path_cache_single;
    /*原始图片缓存路径*/
    public static String Path_cache_original;

    public static void initFolder(Context context) {
        Utils.Path_cache_gallery = context.getExternalCacheDir().getPath() + "/galleryMicroPics/";
        if (!CUtils.fileIsExists(Utils.Path_cache_gallery)) {
            File f = new File(Utils.Path_cache_gallery);
            f.mkdirs();
        }

        Utils.Path_cache_single = context.getExternalCacheDir().getPath() + "/singleMicroPics/";
        if (!CUtils.fileIsExists(Utils.Path_cache_single)) {
            File f = new File(Utils.Path_cache_single);
            f.mkdirs();
        }

        Utils.Path_cache_original = context.getExternalCacheDir().getPath() + "/originalPics/";
        if (!CUtils.fileIsExists(Utils.Path_cache_original)) {
            File f = new File(Utils.Path_cache_original);
            f.mkdirs();
        }
    }

    /**
     * 判断是否开启了自动亮度调节
     */
    public static boolean IsAutoBrightness(Context context) {
        boolean IsAutoBrightness = false;
        try {
            IsAutoBrightness = android.provider.Settings.System.getInt(
                    context.getContentResolver(),
                    android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE) == android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
        } catch (android.provider.Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        return IsAutoBrightness;
    }

    /**
     * 停止自动亮度调节
     */
    public static void stopAutoBrightness(Context context) {
        android.provider.Settings.System.putInt(context.getContentResolver(),
                android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE,
                android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
    }

    /**
     * 开启亮度自动调节
     */
    public static void startAutoBrightness(Context context) {
        android.provider.Settings.System.putInt(context.getContentResolver(),
                android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE,
                android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
    }

    /**
     * 设置当前界面的亮度
     */
    public static void setCurWindowBrightness(Context context, int brightness) {
        // 如果开启自动亮度，则关闭。否则，设置了亮度值也是无效的
        if (IsAutoBrightness(context)) {
            stopAutoBrightness(context);
        }
        // context转换为Activity
        Activity activity = (Activity) context;
        WindowManager.LayoutParams lp = activity.getWindow().getAttributes();

        // 异常处理
        if (brightness < 1)
            brightness = 1;
        if (brightness > 255)
            brightness = 255;
        lp.screenBrightness = brightness / 255f;
        activity.getWindow().setAttributes(lp);
    }
}
