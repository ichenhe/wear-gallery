package cc.chenhe.weargallery.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import cc.chenhe.weargallery.R;

/**
 * 用于图片菜单的分割线
 * Created by 晨鹤 on 2017/1/2.
 */

public class RoundSplitView extends View {

    int mLineLength;
    int mLineWidth;
    int mLineColor;
    int mInnerR;
    Paint mPaint;

    public RoundSplitView(Context context) {
        this(context, null);
    }

    public RoundSplitView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RoundSplitView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.getTheme().
                obtainStyledAttributes(attrs
                        , R.styleable.RoundSplitView
                        , defStyleAttr, 0);
        mPaint = new Paint();

        try {
            mLineLength = a.getDimensionPixelOffset(R.styleable.RoundSplitView_lineLength, 0);
            mLineWidth = a.getDimensionPixelOffset(R.styleable.RoundSplitView_lineWidth, 0);
            mInnerR = a.getDimensionPixelOffset(R.styleable.RoundSplitView_innerR, 0);
            mLineColor = a.getColor(R.styleable.RoundSplitView_lineColor, Color.WHITE);
        } finally {
            a.recycle();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //获取控件大小
        int width, height, r;
        width = getMeasuredWidth();
        height = getMeasuredHeight();
        r = width > height ? height / 2 : width / 2;

        if (mLineLength > r)
            mLineLength = r;
        mPaint.setColor(mLineColor);
        mPaint.setStrokeWidth(mLineWidth);

//        canvas.drawRect(r - mLineWidth / 2
//                , (r - mInnerR) / 2 - mLineLength / 2
//                , r + mLineWidth / 2
//                , (r - mInnerR) / 2 + mLineLength / 2
//                , mPaint);
        float x1, y1, x2, y2, x3, y3, x4, y4;
        x1 = r;
        y1 = (r - mInnerR) / 2 - mLineLength / 2;
        x2 = r;
        y2 = (r - mInnerR) / 2 + mLineLength / 2;

        x3 = (float) (x1 - (r - y1) / 2 * Math.sqrt(3));
        y3 = r + (r - y1) / 2;
        x4 = (float) (x2 - (r - y2) / 2 * Math.sqrt(3));
        y4 = r + (r - y2) / 2;
        canvas.drawLine(x1, y1
                , x2, y2, mPaint);
        canvas.drawLine(x3, y3
                , x4, y4, mPaint);
        canvas.drawLine(x3 + 2 * (x1 - x3), y3
                , x4 + 2 * (x1 - x4), y4, mPaint);

    }
}
