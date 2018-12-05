package cc.chenhe.weargallery.ui;

import android.content.Context;
import android.support.wearable.view.drawer.WearableDrawerLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * 实现当 enable = false 时禁止下拉弹出。
 */
@SuppressWarnings("deprecation")
public class MyWearableDrawerLayout extends WearableDrawerLayout {
    public MyWearableDrawerLayout(Context context) {
        super(context);
    }

    public MyWearableDrawerLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyWearableDrawerLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public MyWearableDrawerLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return isEnabled() && super.onInterceptTouchEvent(ev);
    }
}
