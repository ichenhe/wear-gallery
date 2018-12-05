package cc.chenhe.weargallery.ui;

import android.content.Context;
import android.support.wearable.view.GridViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

/**
 * Created by 晨鹤 on 2016/12/31.
 */

public class MyGridViewPager extends GridViewPager {
    public MyGridViewPager(Context context) {
        super(context);
    }

    public MyGridViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyGridViewPager(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
//        if (ev.getAction() == MotionEvent.ACTION_DOWN)
//            Log.i("ViewPager", "--dispatchTouchEvent-- :DOWN");
//        if (ev.getAction() == MotionEvent.ACTION_MOVE)
//            Log.i("ViewPager", "--dispatchTouchEvent-- :MOVE");
//        if (ev.getAction() == MotionEvent.ACTION_UP)
//            Log.i("ViewPager", "--dispatchTouchEvent-- :UP");
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
//        if (ev.getAction() == MotionEvent.ACTION_DOWN)
//            Log.i("ViewPager", "--onTouchEvent-- :DOWN");
//        if (ev.getAction() == MotionEvent.ACTION_MOVE)
//            Log.i("ViewPager", "--onTouchEvent-- :MOVE");
//        if (ev.getAction() == MotionEvent.ACTION_UP)
//            Log.i("ViewPager", "--onTouchEvent-- :UP");
        return super.onTouchEvent(ev);
    }

}
