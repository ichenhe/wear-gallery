package cc.chenhe.weargallery.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.request.RequestOptions;

import cc.chenhe.weargallery.GlideApp;
import cc.chenhe.weargallery.R;
import cc.chenhe.weargallery.uilts.Settings;

public class AtySwitchGuide extends WearableActivity implements View.OnClickListener {

    public static void start(Context context) {
        Intent intent = new Intent(context, AtySwitchGuide.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.aty_switch_watchface_guide);
        ((TextView) findViewById(R.id.tvMessage))
                .setText(getString(R.string.wf_switch_guide,
                        getString(R.string.app_name)));
        GlideApp.with(this)
                .load(R.drawable.wf_switch_guide)
                .apply(RequestOptions.circleCropTransform())
                .into((ImageView) findViewById(R.id.tvSwitchWatchfaceGuide));
        findViewById(R.id.tvBtnNoMoreShow).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.tvBtnNoMoreShow) {
            Settings.showSwitchWfTip(this, false);
            finish();
        }
    }
}
