package cc.chenhe.weargallery.utils;

import android.content.Context;
import android.graphics.Color;

import com.afollestad.materialdialogs.MaterialDialog;

import cc.chenhe.weargallery.R;

/**
 * Created by 晨鹤 on 2016/12/13.
 */

public class DialogUtils {
    public static MaterialDialog.Builder getProgressDialogBuilder(Context context) {
        return new MaterialDialog.Builder(context)
                .widgetColor(context.getResources().getColor(R.color.colorAccent))
                .progress(true, 0);
    }

    public static MaterialDialog.Builder getBasicDialogBuilder(Context context) {
        return new MaterialDialog.Builder(context);
    }
}
