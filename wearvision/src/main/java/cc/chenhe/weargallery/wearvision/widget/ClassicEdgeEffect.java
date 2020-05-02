/*
 * Copyright (C) 2016 Mobvoi Inc.
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;

/**
 * A classic edge effect will draw a arc pointer to content, with single fill color.
 */
public class ClassicEdgeEffect extends EdgeEffect {
    @SuppressWarnings("UnusedDeclaration")
    private static final String TAG = "ClassicEdgeEffect";

    private static final double ANGLE = Math.PI / 6;
    private static final float SIN = (float) Math.sin(ANGLE);
    private static final float COS = (float) Math.cos(ANGLE);

    private final Paint mPaint = new Paint();
    private float mRadius;
    private float mBaseGlowScale;

    public ClassicEdgeEffect(Context context) {
        super(context);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
        updatePaint();
    }

    private void updatePaint() {
        int themeColor = getColor();
        mPaint.setColor((themeColor & 0xffffff) | 0x33000000);
    }

    /**
     * Set the size of this edge effect in pixels.
     *
     * @param width  Effect width in pixels
     * @param height Effect height in pixels
     */
    @Override
    public void setSize(int width, int height) {
        final float r = width * 0.75f / SIN;
        final float y = COS * r;
        final float h = r - y;
        final float or = height * 0.75f / SIN;
        final float oy = COS * or;
        final float oh = or - oy;

        mRadius = r;
        mBaseGlowScale = h > 0 ? Math.min(oh / h, 1.f) : 1.f;

        Rect bounds = getBounds();
        bounds.set(bounds.left, bounds.top, width, (int) Math.min(height, h));
    }

    @Override
    public void setColor(int color) {
        super.setColor(color);
        updatePaint();
    }

    /**
     * Draw into the provided canvas. Assumes that the canvas has been rotated
     * accordingly and the size has been set. The effect will be drawn the full
     * width of X=0 to X=width, beginning from Y=0 and extending to some factor <
     * 1.f of height.
     *
     * @param canvas Canvas to draw into
     */
    @Override
    public void onDraw(Canvas canvas) {
        final int count = canvas.save();

        final Rect bounds = getBounds();
        final float centerX = bounds.centerX();
        final float centerY = bounds.height() - mRadius;

        canvas.scale(1.f, Math.min(getGlowScaleY(), 1.f) * mBaseGlowScale, centerX, 0);

        final float displacement = Math.max(0, Math.min(getDisplacement(), 1.f)) - 0.5f;
        float translateX = bounds.width() * displacement / 2;

        canvas.clipRect(bounds);
        canvas.translate(translateX, 0);
        mPaint.setAlpha((int) (0x7f * getGlowAlpha()));
        canvas.drawCircle(centerX, centerY, mRadius, mPaint);
        canvas.restoreToCount(count);
    }

}
