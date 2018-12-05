package cc.chenhe.weargallery.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.CompoundButton;

import org.greenrobot.eventbus.EventBus;

import cc.chenhe.weargallery.R;
import cc.chenhe.weargallery.bean.eventmsg.WatchfaceConfigChangedMsg;
import cc.chenhe.weargallery.uilts.WfSettings;
import ticwear.design.widget.SimpleSwitch;

public class AtyWatchFaceSetting extends Activity {
    Context context;
    private SimpleSwitch ssShowAmbient;

    @Override
    protected void onDestroy() {
        EventBus.getDefault().post(new WatchfaceConfigChangedMsg());
        super.onDestroy();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.aty_watchface_config);
        context = this;
        ssShowAmbient = findViewById(R.id.ssAmbientVisible);

        initData();
        initEvent();
    }

    private void initData() {
        ssShowAmbient.setChecked(WfSettings.showInAmbient(context));
    }

    private void initEvent() {
        CompoundButton.OnCheckedChangeListener checkedChangeListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                switch (buttonView.getId()) {
                    case R.id.ssAmbientVisible:
                        WfSettings.showInAmbient(context, isChecked);
                        break;
                }
            }
        };
        ssShowAmbient.setOnCheckedChangeListener(checkedChangeListener);

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
                }
            }
        };
        findViewById(R.id.layoutSetImage).setOnClickListener(clickListener);
        findViewById(R.id.layoutAmbientVisible).setOnClickListener(clickListener);
    }
}
