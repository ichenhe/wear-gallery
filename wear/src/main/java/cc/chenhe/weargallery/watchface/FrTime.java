package cc.chenhe.weargallery.watchface;

import android.content.Context;
import android.view.View;
import android.widget.CompoundButton;

import org.greenrobot.eventbus.EventBus;

import cc.chenhe.weargallery.R;
import cc.chenhe.weargallery.bean.eventmsg.WatchfaceConfigChangedMsg;
import cc.chenhe.weargallery.fragment.FrSwipeDismiss;
import cc.chenhe.weargallery.uilts.WfSettings;
import ticwear.design.widget.SimpleSwitch;

public final class FrTime extends FrSwipeDismiss {
    private SimpleSwitch ssAnalogMode;

    Context context;

    public static FrTime create() {
        return new FrTime();
    }

    @Override
    protected int onGetContentViewId() {
        return R.layout.wf_fr_time;
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
        ssAnalogMode = findViewById(R.id.ssTimeAnalogMode);
        ssAnalogMode.setChecked(WfSettings.analogMode(context));
        setDigitalModeConfigVisible(!ssAnalogMode.isChecked());
        initEvent();
    }

    private void initEvent() {
        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.layoutTimePosition:
                        switchFragment(R.id.frame, FrTimePosition.create());
                        break;
                    case R.id.layoutTimeContent:
                        switchFragment(R.id.frame, FrTimeContent.create());
                        break;
                    case R.id.layoutTimeColor:
                        switchFragment(R.id.frame, FrTimeColor.create());
                        break;
                    case R.id.layoutTimeAnalogMode:
                        ssAnalogMode.setChecked(!ssAnalogMode.isChecked());
                        break;
                }
            }
        };
        findViewById(R.id.layoutTimePosition).setOnClickListener(clickListener);
        findViewById(R.id.layoutTimeContent).setOnClickListener(clickListener);
        findViewById(R.id.layoutTimeColor).setOnClickListener(clickListener);
        findViewById(R.id.layoutTimeAnalogMode).setOnClickListener(clickListener);

        ssAnalogMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                WfSettings.analogMode(context, isChecked);
                setDigitalModeConfigVisible(!isChecked);
            }
        });
    }

    private void setDigitalModeConfigVisible(boolean visible) {
        findViewById(R.id.layoutTimePosition).setVisibility(visible ? View.VISIBLE : View.GONE);
        findViewById(R.id.layoutTimeContent).setVisibility(visible ? View.VISIBLE : View.GONE);
        findViewById(R.id.layoutTimeColor).setVisibility(visible ? View.VISIBLE : View.GONE);
    }
}
