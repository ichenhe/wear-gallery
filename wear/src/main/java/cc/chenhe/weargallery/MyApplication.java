package cc.chenhe.weargallery;

import android.app.Application;
import android.widget.Toast;

import com.mobvoi.android.common.NoAvailableServiceException;
import com.tencent.bugly.Bugly;
import com.tencent.bugly.crashreport.CrashReport;

import cc.chenhe.lib.weartools.WTUtils;
import cc.chenhe.weargallery.uilts.Utils;

/**
 * Created by 晨鹤 on 2016/12/17.
 */

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Utils.initFolder(getApplicationContext());
        initWearTools();

        CrashReport.setIsDevelopmentDevice(getApplicationContext(), BuildConfig.DEBUG);
        Bugly.init(getApplicationContext(), BuildConfig.BUGLY_ID, BuildConfig.DEBUG);
    }

    private void initWearTools() {
        try {
            WTUtils.init(getApplicationContext(), WTUtils.MODE_AUTO);
            WTUtils.setBothwayTimeOut(15 * 1000);
            WTUtils.setDebug(BuildConfig.DEBUG);
        } catch (NoAvailableServiceException e) {
            Toast.makeText(this, "neither mms nor gms is avaliable.", Toast.LENGTH_SHORT).show();
        }
    }
}
