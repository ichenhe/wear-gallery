package cc.chenhe.weargallery;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.mobvoi.android.common.MobvoiApiManager;

import org.greenrobot.eventbus.EventBus;

import java.util.Objects;

import cc.chenhe.lib.weartools.WTBothway;
import cc.chenhe.weargallery.bean.eventmsg.ShowWatchRequestChangedMsg;
import cc.chenhe.weargallery.common.CUtils;
import cc.chenhe.weargallery.common.Cc;
import cc.chenhe.weargallery.utils.DialogUtils;
import cc.chenhe.weargallery.utils.HideDbHelper;
import cc.chenhe.weargallery.utils.Settings;
import cc.chenhe.weargallery.utils.Utils;

/**
 * Created by 晨鹤 on 2016/12/12.
 */

public class AtySetting extends AtyBase {
    public static final int RESULT_NEED_REFRESH = 1;
    public static final int RESULT_NORMAL = 0;

    private static final int REQUEST_HIDE = 0;
    private Context context;
    private TextView tvCurrentMode, tvSizeCache, tvSizeImages, tvHideCount;
    private Switch swToast, swForceAw;
    private View llUpdate, llHide, llDonate, llAbout;

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setResult(RESULT_NORMAL);
        setContentView(R.layout.aty_setting);
        initView();
        initData();
        initEvent();
    }

    private void getWatchCacheSize() {
        WTBothway.request(context, Cc.PATH_GET_WATCH_CACHE_SIZE, "", new WTBothway.BothwayCallback() {
            @Override
            public void onRespond(byte[] data) {
                if (new String(data).equals("err")) {
                    tvSizeCache.setText(getString(R.string.setting_calculate_cache_size_err));
                    return;
                }
                String[] sizes = new String(data).split("\\|");
                if (sizes.length ==2) {
                    tvSizeCache.setText(getString(R.string.setting_cache_size, sizes[0]));
                    tvSizeImages.setText(getString(R.string.setting_cache_size, sizes[1]));
                }
            }

            @Override
            public void onFailed(int resultCode) {
                tvSizeCache.setText("");
                tvSizeImages.setText("");
            }
        });
    }

    private void initData() {
        MobvoiApiManager.ApiGroup group = MobvoiApiManager.getInstance().getGroup();
        boolean isInitialized = MobvoiApiManager.getInstance().isInitialized();
        if (isInitialized && group.equals(MobvoiApiManager.ApiGroup.GMS)) {
            tvCurrentMode.setText(getString(R.string.setting_current_mode, getString(R.string.mode_gms)));
        } else if (isInitialized && group.equals(MobvoiApiManager.ApiGroup.MMS)) {
            tvCurrentMode.setText(getString(R.string.setting_current_mode, getString(R.string.mode_mms)));
        } else {
            tvCurrentMode.setText(getString(R.string.setting_current_mode, getString(R.string.mode_unknown)));
        }

        swToast.setChecked(Settings.showWatchRequestToast(context));
        swForceAw.setTag(Settings.forceTicMode(context));
        swForceAw.setChecked(Settings.forceTicMode(context));
        refreshHideCount();

        getWatchCacheSize();
    }

    /**
     * 刷新已隐藏相册个数
     */
    private void refreshHideCount() {
        SQLiteDatabase database = HideDbHelper.getInstance(getApplicationContext()).getReadableDatabase();
        Cursor cursor = database.query(HideDbHelper.TABLE_HADE_NAME, null, null, null, null, null, null);
        if (cursor == null) {
            Toast.makeText(context, getString(R.string.err_database), Toast.LENGTH_SHORT).show();
            database.close();
            finish();
            return;
        }
        tvHideCount.setText(getString(R.string.setting_hide_gallery_num, cursor.getCount()));
        cursor.close();
        database.close();
    }

    private void initEvent() {
        CompoundButton.OnCheckedChangeListener checkedChangeListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                switch (buttonView.getId()) {
                    case R.id.sb_toast:
                        Settings.showWatchRequestToast(context, isChecked);
                        EventBus.getDefault().post(new ShowWatchRequestChangedMsg(isChecked));
                        break;
                    case R.id.sb_force_aw:
                        changeMode(buttonView, isChecked);
                        break;
                }
            }
        };
        swToast.setOnCheckedChangeListener(checkedChangeListener);
        swForceAw.setOnCheckedChangeListener(checkedChangeListener);

        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.tv_del_cache:
                    case R.id.tv_del_images:
                        do_delWatchCache(v.getId());
                        break;
                    case R.id.llUpdate:
                        openMarket(context);
                        break;
                    case R.id.llHade:
                        startActivityForResult(new Intent(context, AtyHideFolders.class), REQUEST_HIDE);
                        break;
                    case R.id.llDonate:
                        toAliPayScan();
                        break;
                    case R.id.rl_toast:
                        swToast.setChecked(!swToast.isChecked());
                        break;
                    case R.id.rl_force_aw:
                        swForceAw.setChecked(!swForceAw.isChecked());
                        break;
                    case R.id.llAbout:
                        startActivity(new Intent(context, AtyAndroidLinks.class));
                        break;
                    case R.id.tv_force_aw_what:
                        new MaterialDialog.Builder(context)
                                .title(R.string.tip)
                                .content(R.string.setting_force_tw_tip)
                                .positiveText(R.string.confirm)
                                .show();
                        break;
                }
            }
        };
        findViewById(R.id.tv_del_cache).setOnClickListener(clickListener);
        findViewById(R.id.tv_del_images).setOnClickListener(clickListener);
        llHide.setOnClickListener(clickListener);
        llAbout.setOnClickListener(clickListener);
        findViewById(R.id.rl_toast).setOnClickListener(clickListener);
        findViewById(R.id.rl_force_aw).setOnClickListener(clickListener);
        llUpdate.setOnClickListener(clickListener);
        llDonate.setOnClickListener(clickListener);
        findViewById(R.id.tv_force_aw_what).setOnClickListener(clickListener);
    }

    private void changeMode(final CompoundButton button, final boolean isChecked) {
        if ((boolean) button.getTag() == isChecked)
            return;
        new MaterialDialog.Builder(context)
                .title(R.string.tip)
                .content(R.string.setting_force_tw_restart)
                .positiveText(R.string.confirm)
                .negativeText(R.string.cancel)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        Settings.forceTicMode(context, isChecked);
                        button.setTag(isChecked);
                        android.os.Process.killProcess(android.os.Process.myPid());
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        button.setChecked(!isChecked);
                    }
                })
                .show();
    }

    /**
     * 调用支付宝支付
     */
    private void toAliPayScan() {
        try {
            //利用Intent打开支付宝
            Uri uri = Uri.parse("alipayqr://platformapi/startapp?saId=10000007&qrcode=https://qr.alipay.com/a6x02024auqjunm2vc6ojc6");
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        } catch (Exception e) {
            //若无法正常跳转
            DialogUtils.getBasicDialogBuilder(context)
                    .title("Oops")
                    .content("拉起支付宝异常。您可以手动转账至1059836454@qq.com来捐助。谢谢亲~")
                    .positiveText("好的")
                    .show();
        }
    }

    private void do_delWatchCache(final int viewId) {
        String content = "";
        switch (viewId) {
            case R.id.tv_del_cache:
                content = getString(R.string.setting_del_watch_cache);
                break;
            case R.id.tv_del_images:
                content = getString(R.string.setting_del_watch_images);
                break;
        }

        final MaterialDialog pDialog = DialogUtils.getProgressDialogBuilder(context)
                .content(getString(R.string.setting_del_cache_ing))
                .cancelable(false)
                .build();
        DialogUtils.getBasicDialogBuilder(context)
                .title(R.string.setting_del_cache_watch)
                .content(content)
                .positiveText(R.string.setting_del_cache_watch_pos)
                .negativeText(R.string.setting_del_cache_watch_neg)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        tvSizeCache.setText("");
                        tvSizeImages.setText("");
                        pDialog.show();
                        String path = "";
                        switch (viewId) {
                            case R.id.tv_del_cache:
                                path = Cc.PATH_DEL_CACHE;
                                break;
                            case R.id.tv_del_images:
                                path = Cc.PATH_DEL_OFFLINE_IMAGE;
                                break;
                        }
                        WTBothway.request(context, path, "", new WTBothway.BothwayCallback() {
                            @Override
                            public void onRespond(byte[] data) {
                                pDialog.dismiss();
                                getWatchCacheSize();
                                DialogUtils.getBasicDialogBuilder(context)
                                        .content(R.string.setting_del_cache_ok)
                                        .positiveText(R.string.confirm)
                                        .show();
                            }

                            @Override
                            public void onFailed(int resultCode) {
                                pDialog.dismiss();
                                DialogUtils.getBasicDialogBuilder(context)
                                        .title(R.string.setting_del_watch_cache_communicate_err)
                                        .content(R.string.setting_del_watch_cache_communicate_err_des)
                                        .positiveText(R.string.confirm)
                                        .show();
                            }
                        });
                    }
                })
                .show();
    }

    private void openMarket(@NonNull Context context) {
        try {
            Uri uri = Uri.parse("market://details?id=" + context.getPackageName());
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            openWithBrowser(context, Utils.UPDATE_URL);
        }
    }

    public static void openWithBrowser(Context context, String url) {
        try {
            context.startActivity(Intent.createChooser(new Intent(Intent.ACTION_VIEW, Uri.parse(url)),
                    context.getString(R.string.links_chooser_browser)));
        } catch (ActivityNotFoundException ignored) {
            Toast.makeText(context, R.string.update_no_web_view, Toast.LENGTH_SHORT).show();
        }
    }

    private void initView() {
        tvCurrentMode = findViewById(R.id.tvCurrentMode);
        tvSizeCache = findViewById(R.id.tvSizeCache);
        tvSizeImages = findViewById(R.id.tvSizeImages);
        tvHideCount = findViewById(R.id.tvHadeCount);

        Toolbar bar = findViewById(R.id.bar);
        bar.setTitle(getString(R.string.setting_title));
        setSupportActionBar(bar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowHomeEnabled(true);

        swToast = findViewById(R.id.sb_toast);
        swForceAw = findViewById(R.id.sb_force_aw);

        llUpdate = findViewById(R.id.llUpdate);
        llHide = findViewById(R.id.llHade);
        llDonate = findViewById(R.id.llDonate);
        llAbout = findViewById(R.id.llAbout);

        ((TextView) findViewById(R.id.updateVersion)).setText(CUtils.getVersionName(context));

        //在非中文模式下屏蔽捐助按钮
        Configuration config = getResources().getConfiguration();
        if (!config.locale.getLanguage().equals("zh")) {
            llDonate.setVisibility(View.GONE);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_HIDE) {
            if (resultCode == 0) {
                setResult(RESULT_NORMAL);
            } else {
                refreshHideCount();
                setResult(RESULT_NEED_REFRESH);
            }
        }
    }
}
