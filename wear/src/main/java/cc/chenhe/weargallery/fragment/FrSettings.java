package cc.chenhe.weargallery.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;

import java.io.File;

import cc.chenhe.weargallery.R;
import cc.chenhe.weargallery.activity.AtyWebServer;
import cc.chenhe.weargallery.bean.eventmsg.LocalImageChangedMsg;
import cc.chenhe.weargallery.common.CUtils;
import cc.chenhe.weargallery.ui.AlertDialog;
import cc.chenhe.weargallery.uilts.Settings;
import cc.chenhe.weargallery.uilts.Utils;
import ticwear.design.widget.SimpleSwitch;

/**
 * 简单设置界面
 * Created by 晨鹤 on 2016/6/16.
 */
public class FrSettings extends FrBase {
    private Activity activity;
    private Context context;
    private SimpleSwitch ssAlwaysOn, ssMaxBrightness;
    private SharedPreferences sp;
    private View rlAlwaysOn, rlMaxBrightness;

    @Override
    protected int onGetContentViewId() {
        return R.layout.fr_settings;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        activity = (Activity) context;
    }

    @Override
    protected void onLazyLoad() {
        super.onLazyLoad();
        sp = context.getSharedPreferences(Settings.NAME, Context.MODE_PRIVATE);

        ssAlwaysOn = findViewById(R.id.ssAlwaysOn);
        ssMaxBrightness = findViewById(R.id.ssMaxBrightness);
        rlAlwaysOn = findViewById(R.id.layoutAlwaysOn);
        rlMaxBrightness = findViewById(R.id.layoutMaxBrightness);

        initData();
        initEvent();
    }

    private void initData() {
        ssAlwaysOn.setChecked(sp.getBoolean(Settings.ITEM_WATCH_WEAK_LOCK, Settings.ITEM_WATCH_WEAK_LOCK_D));
        ssMaxBrightness.setChecked(sp.getBoolean(Settings.ITEM_MAX_BRIGHTNESS, Settings.ITEM_MAX_BRIGHTNESS_D));
    }

    private void initEvent() {
        final SharedPreferences.Editor editor = sp.edit();
        CompoundButton.OnCheckedChangeListener checkedChangeListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                switch (buttonView.getId()) {
                    case R.id.ssAlwaysOn:
                        editor.putBoolean(Settings.ITEM_WATCH_WEAK_LOCK, isChecked);
                        if (isChecked && Settings.showAlwaysOnTip(context))
                            new AlertDialog.Builder(context)
                                    .setTitle(R.string.tip)
                                    .setMessage(R.string.settings_dialog_always_on)
                                    .setPositiveButtonListener(new AlertDialog.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Settings.showAlwaysOnTip(context, false);
                                        }
                                    })
                                    .show();
                        break;
                    case R.id.ssMaxBrightness:
                        if (isChecked) {
                            //检查权限
                            if (Build.VERSION.SDK_INT >= 23 && !android.provider.Settings.System.canWrite(context)) {
                                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                        Uri.parse("package:" + activity.getPackageName()));
                                try {
                                    startActivityForResult(intent, 1);
                                } catch (Exception e) {
                                    buttonView.setChecked(false);
                                    Toast.makeText(context, R.string.settings_err_permission_write_settings_content_not_support, Toast.LENGTH_SHORT).show();
                                }
                                return;
                            }
                        }
                        editor.putBoolean(Settings.ITEM_MAX_BRIGHTNESS, isChecked);
                        break;
                }
                editor.apply();
            }
        };
        ssAlwaysOn.setOnCheckedChangeListener(checkedChangeListener);
        ssMaxBrightness.setOnCheckedChangeListener(checkedChangeListener);

        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.layoutAlwaysOn:
                        ssAlwaysOn.setChecked(!ssAlwaysOn.isChecked());
                        break;
                    case R.id.layoutMaxBrightness:
                        ssMaxBrightness.setChecked(!ssMaxBrightness.isChecked());
                        break;
                    case R.id.layoutLanTransfer:
                        startActivity(new Intent(context, AtyWebServer.class));
                        break;
                    case R.id.layoutDelCache:
                        CUtils.deleteFile(new File(Utils.Path_cache_gallery));
                        CUtils.deleteFile(new File(Utils.Path_cache_single));
                        Toast.makeText(context, R.string.settings_del_all_cache_ok, Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.layoutDelLocalImage:
                        deleteLocalImage();
                        break;
                    case R.id.layoutAbout:
                        switchFragment(R.id.frame,new FrAbout());
                        break;
                }
            }
        };
        rlAlwaysOn.setOnClickListener(clickListener);
        rlMaxBrightness.setOnClickListener(clickListener);
        findViewById(R.id.layoutLanTransfer).setOnClickListener(clickListener);
        findViewById(R.id.layoutDelCache).setOnClickListener(clickListener);
        findViewById(R.id.layoutDelLocalImage).setOnClickListener(clickListener);
        findViewById(R.id.layoutAbout).setOnClickListener(clickListener);
    }

    private void deleteLocalImage() {
        new AlertDialog.Builder(context)
                .setTitle(R.string.tip)
                .setMessage(R.string.settings_del_local_image_confirm)
                .setShowNegativeButton(true)
                .setPositiveButtonListener(new AlertDialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        CUtils.deleteFile(new File(Utils.Path_cache_original));
                        EventBus.getDefault().post(new LocalImageChangedMsg());
                        Toast.makeText(context, R.string.settings_del_all_cache_ok, Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            //最大亮度权限
            if (Build.VERSION.SDK_INT >= 23 && !android.provider.Settings.System.canWrite(context)) {
                //未授权
                ssMaxBrightness.setChecked(false);
                new AlertDialog.Builder(context)
                        .setTitle(R.string.settings_err_permission_write_settings_title)
                        .setMessage(R.string.settings_err_permission_write_settings_content)
                        .setPositiveButtonListener(new AlertDialog.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                        Uri.parse("package:" + activity.getPackageName()));
                                try {
                                    startActivityForResult(intent, 1);
                                } catch (Exception ignored) {
                                }
                            }
                        })
                        .setShowNegativeButton(true)
                        .show();
            } else {
                sp.edit().putBoolean(Settings.ITEM_MAX_BRIGHTNESS, true).apply();
            }
        }
    }

}
