package cc.chenhe.weargallery.watchface;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.CompoundButton;

import org.greenrobot.eventbus.EventBus;

import cc.chenhe.weargallery.R;
import cc.chenhe.weargallery.activity.AtyMain;
import cc.chenhe.weargallery.bean.eventmsg.WatchfaceConfigChangedMsg;
import cc.chenhe.weargallery.fragment.FrBase;
import cc.chenhe.weargallery.uilts.WfSettings;
import ticwear.design.widget.SimpleSwitch;

public final class FrConfig extends FrBase {
    Context context;
    private SimpleSwitch ssShowAmbient, ssShowTime;

    public static FrConfig create() {
        return new FrConfig();
    }

    @Override
    protected int onGetContentViewId() {
        return R.layout.wf_fr_config;
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().post(new WatchfaceConfigChangedMsg());
        super.onDestroy();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    protected void onLazyLoad() {
        super.onLazyLoad();
        ssShowAmbient = findViewById(R.id.ssAmbientVisible);
        ssShowTime = findViewById(R.id.ssTimeVisible);
        initData();
        initEvent();
    }

    private void initData() {
        ssShowAmbient.setChecked(WfSettings.showInAmbient(context));
        ssShowTime.setChecked(WfSettings.showTime(context));
    }

    private void initEvent() {
        CompoundButton.OnCheckedChangeListener checkedChangeListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                switch (buttonView.getId()) {
                    case R.id.ssAmbientVisible:
                        WfSettings.showInAmbient(context, isChecked);
                        break;
                    case R.id.ssTimeVisible:
                        WfSettings.showTime(context, isChecked);
                        break;
                }
            }
        };
        ssShowAmbient.setOnCheckedChangeListener(checkedChangeListener);
        ssShowTime.setOnCheckedChangeListener(checkedChangeListener);

        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.layoutSetImage:
                        startActivity(new Intent(context, AtyMain.class));
                        break;
                    case R.id.layoutAmbientVisible:
                        ssShowAmbient.setChecked(!ssShowAmbient.isChecked());
                        break;
                    case R.id.layoutTimeVisible:
                        ssShowTime.setChecked(!ssShowTime.isChecked());
                        break;
                    case R.id.layoutTimeStyle:
                        switchFragment(R.id.frame, FrTime.create());
                        break;
                }
            }
        };
        findViewById(R.id.layoutSetImage).setOnClickListener(clickListener);
        findViewById(R.id.layoutAmbientVisible).setOnClickListener(clickListener);
        findViewById(R.id.layoutTimeVisible).setOnClickListener(clickListener);
        findViewById(R.id.layoutTimeStyle).setOnClickListener(clickListener);
    }
}
