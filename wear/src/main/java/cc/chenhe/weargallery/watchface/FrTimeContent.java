package cc.chenhe.weargallery.watchface;

import android.content.Context;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.widget.EditText;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import cc.chenhe.weargallery.R;
import cc.chenhe.weargallery.fragment.FrSwipeDismiss;
import cc.chenhe.weargallery.uilts.WfSettings;

public class FrTimeContent extends FrSwipeDismiss {

    Context context;
    EditText editText;

    public static FrTimeContent create() {
        return new FrTimeContent();
    }

    @Override
    protected int onGetContentViewId() {
        return R.layout.wf_fr_time_content;
    }

    @Override
    public void onDestroy() {
        String text = editText.getText().toString();
        if (!TextUtils.isEmpty(text)) {
            // 如果格式有误则回到默认
            try {
                new SimpleDateFormat(text, Locale.getDefault()).format(new Date());
            } catch (Exception e) {
                text = null;
            }
        }
        WfSettings.timeContent(context, text);
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
        editText = findViewById(R.id.etWfTimeContent);
        ((TextView) findViewById(R.id.tvWfTimeFormat))
                .setMovementMethod(ScrollingMovementMethod.getInstance());
        editText.setText(WfSettings.timeContent(context));
    }
}
