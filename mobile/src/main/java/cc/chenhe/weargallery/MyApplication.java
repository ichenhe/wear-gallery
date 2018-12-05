package cc.chenhe.weargallery;

import android.app.Application;
import android.widget.Toast;

import com.mobvoi.android.common.NoAvailableServiceException;
import com.tencent.bugly.Bugly;
import com.tencent.bugly.crashreport.CrashReport;

import cc.chenhe.lib.weartools.WTUtils;
import cc.chenhe.weargallery.utils.Settings;

/**
 * Created by 晨鹤 on 2016/12/2.
 */

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        initWearTools();

        CrashReport.setIsDevelopmentDevice(getApplicationContext(), BuildConfig.DEBUG);
        Bugly.init(getApplicationContext(), BuildConfig.BUGLY_ID, BuildConfig.DEBUG);
    }

    private void initWearTools() {
        try {
            if (!Settings.forceTicMode(this)) {
                WTUtils.init(getApplicationContext(), WTUtils.MODE_AUTO);
            } else {
                WTUtils.init(getApplicationContext(), WTUtils.MODE_MMS);
            }
            WTUtils.setDebug(BuildConfig.DEBUG);
        } catch (NoAvailableServiceException e) {
            Toast.makeText(this, R.string.app_none_transfer_available, Toast.LENGTH_LONG).show();
        }
    }
}
