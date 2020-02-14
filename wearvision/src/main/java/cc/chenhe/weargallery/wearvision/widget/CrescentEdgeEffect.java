/*
 * Copyright (c) 2016 Mobvoi Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cc.chenhe.weargallery.wearvision.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;

/**
 * Created by tankery on 4/5/16.
 * <p>
 * An edge effect with crescent shape, better look in round layout.
 */
public class CrescentEdgeEffect extends EdgeEffect {

    @SuppressWarnings("UnusedDeclaration")
    private static final String TAG = "CrescentEdgeEffect";

    /**
     * glow-scale = glow-height / container-height
     */
    private static final float GLOW_SCALE = 0.15f;
    /**
     * crescent edge stop is the distribute stop between the center and edge.
     */
    private static final float CRESCENT_EDGE_STOP = 0.5f / (0.5f + GLOW_SCALE);
    private static final float CRESCENT_EDGE_MIDDLE_STOP = 1 - ((1 - CRESCENT_EDGE_STOP) * 0.2f);

    private final Paint mPaint = new Paint();

    private float mGlowCenterX;
    private float mGlowCenterY;
    private float mGlowRadius;

    public CrescentEdgeEffect(Context context) {
        super(context);
        mPaint.setColor(Color.TRANSPARENT);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
    }

    @Override
    public void setSize(int width, int height) {
        Rect bounds = getBounds();
        if (width == bounds.width() && height == bounds.height()) {
            return;
        }

        super.setSize(width, height);
        int size = Math.max(bounds.width(), bounds.height());

        float halfSize = size * 0.5f;
        // crescent edge size set same as bounds size.
        mGlowRadius = halfSize / CRESCENT_EDGE_STOP;

        mGlowCenterX = bounds.width() * 0.5f;
        mGlowCenterY = mGlowRadius;

        updatePaint();
    }

    @Override
    public void setColor(int color) {
        if (color == getColor()) {
            return;
        }
        super.setColor(color);
        updatePaint();
    }

    private void updatePaint() {
        int deepColor = (getColor() & 0xffffff) | 0xff000000;
        int middleColor = (getColor() & 0xffffff) | 0x7f000000;

        RadialGradient radialGradient = new RadialGradient(
                mGlowCenterX, mGlowCenterY, mGlowRadius,
                new int[]{0, 0, middleColor, deepColor},
                new float[]{0, CRESCENT_EDGE_STOP, CRESCENT_EDGE_MIDDLE_STOP, 1f},
                Shader.TileMode.CLAMP
        );

        mPaint.setShader(radialGradient);
    }

    @Override
    public void onDraw(Canvas canvas) {
        final Rect bounds = getBounds();
        final float scaleY = Math.min(getGlowScaleY(), 1.f);
        final float edgeHeight = bounds.height() * GLOW_SCALE;

        final float displacement = Math.max(0, Math.min(getDisplacement(), 1.f)) - 0.5f;

        float translateX = bounds.width() * displacement / 2;
        float translateY = -edgeHeight * (1 - scaleY);

        final int count = canvas.save();

        canvas.clipRect(bounds);
        canvas.translate(translateX, translateY);
        mPaint.setAlpha((int) (0xff * getGlowAlpha()));
        canvas.drawCircle(mGlowCenterX, mGlowCenterY, mGlowRadius, mPaint);
        canvas.restoreToCount(count);
    }
}
