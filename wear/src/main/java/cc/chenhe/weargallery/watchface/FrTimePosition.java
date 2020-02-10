package cc.chenhe.weargallery.watchface;

import android.content.Context;
import android.graphics.Color;
import android.os.Message;
import androidx.annotation.IdRes;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import cc.chenhe.weargallery.R;
import cc.chenhe.weargallery.fragment.FrSwipeDismiss;
import cc.chenhe.weargallery.uilts.WfSettings;

public final class FrTimePosition extends FrSwipeDismiss {
    private static final int CLICK_CRITICAL = 200; //按住多长时间抬起视为点击
    private static final int MSG_WAIT_CLICK = 1;
    private static final int MSG_PERFORM = 2;

    private static final int STATUS_PERFORMING = 1;
    private static final int STATUS_STOP = 2;

    Context context;
    TextView tvTime;
    MyHandler handler;
    SparseIntArray statusMap;
    long downTime; //手指按下的时间

    RelativeLayout.LayoutParams timeLayoutParams;
    float textSize;

    public static FrTimePosition create() {
        return new FrTimePosition();
    }

    @Override
    protected int onGetContentViewId() {
        return R.layout.wf_fr_time_position;
    }

    @Override
    public void onDestroy() {
        WfSettings.timeSize(context, textSize);
        WfSettings.timeLeft(context, timeLayoutParams.leftMargin);
        WfSettings.timeTop(context, timeLayoutParams.topMargin);
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
        handler = new MyHandler(this);
        statusMap = new SparseIntArray();
        textSize = WfSettings.timeSize(context);
        tvTime = findViewById(R.id.tvWfTime);
        tvTime.setText(new SimpleDateFormat(WfSettings.timeContent(context), Locale.getDefault())
                .format(new Date()));
        tvTime.setTextColor(Color.parseColor(WfSettings.timeColor(context)));
        timeLayoutParams = (RelativeLayout.LayoutParams) tvTime.getLayoutParams();
        timeLayoutParams.leftMargin = WfSettings.timeLeft(context);
        timeLayoutParams.topMargin = WfSettings.timeTop(context);
        tvTime.setLayoutParams(timeLayoutParams);
        tvTime.setTextSize(textSize);
        initEvent();
    }

    private void initEvent() {
        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performFunction(v.getId());
            }
        };
        findViewById(R.id.iv_wf_pos_u).setOnClickListener(clickListener);
        findViewById(R.id.iv_wf_pos_d).setOnClickListener(clickListener);
        findViewById(R.id.iv_wf_pos_l).setOnClickListener(clickListener);
        findViewById(R.id.iv_wf_pos_r).setOnClickListener(clickListener);
        findViewById(R.id.iv_wf_size_increase).setOnClickListener(clickListener);
        findViewById(R.id.iv_wf_size_decrease).setOnClickListener(clickListener);

        View.OnTouchListener touchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        downTime = System.currentTimeMillis();
                        handler.sendMessageDelayed(handler.obtainMessage(MSG_WAIT_CLICK, v.getId()),
                                CLICK_CRITICAL);
                        break;
                    case MotionEvent.ACTION_UP:
                        handler.removeMessages(MSG_WAIT_CLICK);
                        statusMap.put(v.getId(), STATUS_STOP);
                        if (System.currentTimeMillis() - downTime < CLICK_CRITICAL)
                            v.performClick();
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        handler.removeMessages(MSG_WAIT_CLICK);
                        statusMap.put(v.getId(), STATUS_STOP);
                        break;
                }
                return true;
            }
        };
        findViewById(R.id.iv_wf_pos_u).setOnTouchListener(touchListener);
        findViewById(R.id.iv_wf_pos_d).setOnTouchListener(touchListener);
        findViewById(R.id.iv_wf_pos_l).setOnTouchListener(touchListener);
        findViewById(R.id.iv_wf_pos_r).setOnTouchListener(touchListener);
        findViewById(R.id.iv_wf_size_increase).setOnTouchListener(touchListener);
        findViewById(R.id.iv_wf_size_decrease).setOnTouchListener(touchListener);

    }

    private void performFunction(@IdRes int viewId) {
        performFunction(viewId, 1);
    }

    private void performFunction(@IdRes int viewId, int step) {
        switch (viewId) {
            case R.id.iv_wf_pos_u:
                timeLayoutParams.topMargin -= step;
                break;
            case R.id.iv_wf_pos_d:
                timeLayoutParams.topMargin += step;
                break;
            case R.id.iv_wf_pos_l:
                timeLayoutParams.leftMargin -= step;
                break;
            case R.id.iv_wf_pos_r:
                timeLayoutParams.leftMargin += step;
                break;
            case R.id.iv_wf_size_increase:
                textSize++;
                break;
            case R.id.iv_wf_size_decrease:
                textSize--;
                break;
        }
        tvTime.setLayoutParams(timeLayoutParams);
        tvTime.setTextSize(textSize);
    }

    private static class MyHandler extends android.os.Handler {
        WeakReference<FrTimePosition> wr;

        MyHandler(FrTimePosition fr) {
            wr = new WeakReference<>(fr);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            FrTimePosition fr = wr.get();
            if (fr == null || fr.isDetached() || msg.obj == null) return;
            int viewId = (int) msg.obj;
            switch (msg.what) {
                case MSG_WAIT_CLICK:
                    fr.statusMap.put(viewId, STATUS_PERFORMING);
                    sendMessageDelayed(obtainMessage(MSG_PERFORM, viewId), 50);
                    break;
                case MSG_PERFORM:
                    fr.performFunction(viewId, 4);
                    if (fr.statusMap.get(viewId) == STATUS_PERFORMING)
                        sendMessageDelayed(obtainMessage(MSG_PERFORM, viewId), 50);
                    break;
            }
        }
    }
}
