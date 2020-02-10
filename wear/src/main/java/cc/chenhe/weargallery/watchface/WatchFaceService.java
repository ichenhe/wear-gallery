package cc.chenhe.weargallery.watchface;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import androidx.annotation.Nullable;
import androidx.palette.graphics.Palette;
import android.text.TextUtils;
import android.view.SurfaceHolder;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import cc.chenhe.lib.watchfacehelper.BaseWatchFaceService;
import cc.chenhe.weargallery.R;
import cc.chenhe.weargallery.bean.eventmsg.WatchfaceConfigChangedMsg;
import cc.chenhe.weargallery.bean.eventmsg.WatchfaceImageChangedMsg;
import cc.chenhe.weargallery.uilts.WfSettings;

public class WatchFaceService extends BaseWatchFaceService {

    Context context;
    int rotate = 0;
    boolean isAmbient;

    /*指针表盘*/
    Paint timePaint;
    Date date;
    SimpleDateFormat simpleDateFormat;
    float timeX, timeY;

    /*模拟表盘*/
    private static final float CENTER_GAP_AND_CIRCLE_RADIUS = 4f;
    private Paint mHourPaint;
    private Paint mMinutePaint;
    private Paint mSecondPaint;
    private Paint mTickAndCirclePaint;
    private float mCenterX, mCenterY;
    private float mSecondHandLength;
    private float sMinuteHandLength;
    private float sHourHandLength;
    /* 通过背景图片计算表盘颜色 */
    private int mWatchHandColor = Color.WHITE;
    private int mWatchHandHighlightColor = Color.RED;
    private int mWatchHandShadowColor = Color.GRAY;

    @Override
    public Engine onCreateEngine() {
        context = this;
        return new MyEngine();
    }

    private class MyEngine extends BaseEngine {
        int width, height;
        Bitmap bg;

        boolean showAmbient;
        boolean showTime;
        boolean analogMode;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            setInteractiveUpdateRateMS(1000);
            date = new Date();
            isAmbient = isInAmbientMode();
            EventBus.getDefault().register(this);
            loadConfig();
        }

        @Override
        public void onDestroy() {
            EventBus.getDefault().unregister(this);
            super.onDestroy();
        }

        @Subscribe()
        public void onConfigChanged(WatchfaceConfigChangedMsg msg) {
            loadConfig();
            invalidate();
        }

        @Subscribe()
        public void onImageChanged(WatchfaceImageChangedMsg msg) {
            loadImage(msg.imageFilePath);
            invalidate();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            this.width = width;
            this.height = height;
            mCenterX = width / 2f;
            mCenterY = height / 2f;
            mSecondHandLength = (float) (mCenterX * 0.875);
            sMinuteHandLength = (float) (mCenterX * 0.75);
            sHourHandLength = (float) (mCenterX * 0.5);

            if (bg == null)
                loadImage(null);
        }

        private void loadImage(@Nullable String imageFilePath) {
            String image = imageFilePath != null ? imageFilePath : WfSettings.getImage(context);
            if (!TextUtils.isEmpty(image) && new File(image).exists()) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(image, options);
                options.inSampleSize = calculateInSample(options, width, height);
                options.inJustDecodeBounds = false;

                if ("image/gif".equals(options.outMimeType)) {
                    Toast.makeText(context, R.string.wf_not_support_gif, Toast.LENGTH_SHORT).show();
                }

                bg = BitmapFactory.decodeFile(image, options);
                rotate = getExifRotateAngle(image);

                double d;
                if (bg.getWidth() > bg.getHeight()) {
                    d = height * 1d / bg.getHeight();
                } else {
                    d = width * 1d / bg.getWidth();
                }
                bg = Bitmap.createScaledBitmap(bg,
                        (int) (d * bg.getWidth()),
                        (int) (d * bg.getHeight()), true);
            } else {
                bg = ((BitmapDrawable) Objects.requireNonNull(getDrawable(R.drawable.preview))).getBitmap();
                bg = Bitmap.createScaledBitmap(bg, width, height, true);
                rotate = 0;
            }

            Palette.from(bg).generate(new Palette.PaletteAsyncListener() {
                @Override
                public void onGenerated(Palette palette) {
                    if (palette != null) {
                        mWatchHandHighlightColor = palette.getVibrantColor(Color.RED);
                        mWatchHandColor = palette.getLightVibrantColor(Color.WHITE);
                        mWatchHandShadowColor = palette.getDarkMutedColor(Color.BLACK);
                        loadConfig();
                    }
                }
            });
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            isAmbient = inAmbientMode;
            updateStyle();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            super.onDraw(canvas, bounds);
            canvas.drawColor(Color.BLACK);

            if (!isInAmbientMode() || showAmbient) {
                if (bg != null) {
                    canvas.save();
                    canvas.rotate(rotate, width / 2, height / 2);
                    canvas.drawBitmap(bg, 0, 0, null);
                    canvas.restore();
                }
            }
            if (analogMode)
                drawTimeAnalog(canvas);
            else
                drawTimeDigital(canvas);
        }

        private void drawTimeDigital(Canvas canvas) {
            if (!showTime) return;
            date.setTime(System.currentTimeMillis());
            canvas.drawText(simpleDateFormat.format(date), timeX, timeY, timePaint);
        }

        private void drawTimeAnalog(Canvas canvas) {
            if (!showTime) return;
            /*
             * Draw ticks. Usually you will want to bake this directly into the photo, but in
             * cases where you want to allow users to select their own photos, this dynamically
             * creates them on top of the photo.
             */
            float innerTickRadius = mCenterX - 10;
            float outerTickRadius = mCenterX;
            for (int tickIndex = 0; tickIndex < 12; tickIndex++) {
                float tickRot = (float) (tickIndex * Math.PI * 2 / 12);
                float innerX = (float) Math.sin(tickRot) * innerTickRadius;
                float innerY = (float) -Math.cos(tickRot) * innerTickRadius;
                float outerX = (float) Math.sin(tickRot) * outerTickRadius;
                float outerY = (float) -Math.cos(tickRot) * outerTickRadius;
                canvas.drawLine(mCenterX + innerX, mCenterY + innerY,
                        mCenterX + outerX, mCenterY + outerY, mTickAndCirclePaint);
            }

            /*
             * These calculations reflect the rotation in degrees per unit of time, e.g.,
             * 360 / 60 = 6 and 360 / 12 = 30.
             */
            final float seconds =
                    (getCalendar().get(Calendar.SECOND) + getCalendar().get(Calendar.MILLISECOND) / 1000f);
            final float secondsRotation = seconds * 6f;

            final float minutesRotation = getCalendar().get(Calendar.MINUTE) * 6f;

            final float hourHandOffset = getCalendar().get(Calendar.MINUTE) / 2f;
            final float hoursRotation = (getCalendar().get(Calendar.HOUR) * 30) + hourHandOffset;

            /*
             * Save the canvas state before we can begin to rotate it.
             */
            canvas.save();

            canvas.rotate(hoursRotation, mCenterX, mCenterY);
            canvas.drawLine(
                    mCenterX,
                    mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
                    mCenterX,
                    mCenterY - sHourHandLength,
                    mHourPaint);

            canvas.rotate(minutesRotation - hoursRotation, mCenterX, mCenterY);
            canvas.drawLine(
                    mCenterX,
                    mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
                    mCenterX,
                    mCenterY - sMinuteHandLength,
                    mMinutePaint);

            /*
             * Ensure the "seconds" hand is drawn only when we are in interactive mode.
             * Otherwise, we only update the watch face once a minute.
             */
            if (!isAmbient) {
                canvas.rotate(secondsRotation - minutesRotation, mCenterX, mCenterY);
                canvas.drawLine(
                        mCenterX,
                        mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
                        mCenterX,
                        mCenterY - mSecondHandLength,
                        mSecondPaint);

            }
            canvas.drawCircle(
                    mCenterX,
                    mCenterY,
                    CENTER_GAP_AND_CIRCLE_RADIUS,
                    mTickAndCirclePaint);

            /* Restore the canvas' original orientation. */
            canvas.restore();
        }

        private void loadConfig() {
            showAmbient = WfSettings.showInAmbient(context);
            analogMode = WfSettings.analogMode(context);
            showTime = WfSettings.showTime(context);
            if (showTime) {
                if (analogMode) {
                    initAnalog();
                } else {
                    initDigital();
                }
            }
        }

        /**
         * 初始化数字表盘。
         */
        private void initDigital() {
            timePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            timeX = WfSettings.timeLeft(context);
            timeY = WfSettings.timeTop(context);
            try {
                timePaint.setColor(Color.parseColor(WfSettings.timeColor(context)));
                timePaint.setTextSize(sp2px(context, WfSettings.timeSize(context)));
                simpleDateFormat = new SimpleDateFormat(WfSettings.timeContent(context),
                        Locale.getDefault());
            } catch (Exception ignore) {
            }
            Paint.FontMetrics fontMetrics = timePaint.getFontMetrics();
            timeY -= fontMetrics.top; //使得和TextView效果一致
        }

        /**
         * 初始化模拟表盘。
         */
        private void initAnalog() {
            mHourPaint = new Paint();
            mHourPaint.setColor(Color.WHITE);
            mHourPaint.setStrokeWidth(5f);
            mHourPaint.setAntiAlias(true);
            mHourPaint.setStrokeCap(Paint.Cap.ROUND);
            mHourPaint.setShadowLayer(6, 0, 0, mWatchHandShadowColor);

            mMinutePaint = new Paint();
            mMinutePaint.setColor(mWatchHandColor);
            mMinutePaint.setStrokeWidth(3f);
            mMinutePaint.setAntiAlias(true);
            mMinutePaint.setStrokeCap(Paint.Cap.ROUND);
            mMinutePaint.setShadowLayer(6, 0, 0, mWatchHandShadowColor);

            mSecondPaint = new Paint();
            mSecondPaint.setColor(mWatchHandHighlightColor);
            mSecondPaint.setStrokeWidth(2f);
            mSecondPaint.setAntiAlias(true);
            mSecondPaint.setStrokeCap(Paint.Cap.ROUND);
            mSecondPaint.setShadowLayer(6, 0, 0, mWatchHandShadowColor);

            mTickAndCirclePaint = new Paint();
            mTickAndCirclePaint.setColor(mWatchHandColor);
            mTickAndCirclePaint.setStrokeWidth(2f);
            mTickAndCirclePaint.setAntiAlias(true);
            mTickAndCirclePaint.setStyle(Paint.Style.STROKE);
            mTickAndCirclePaint.setShadowLayer(6, 0, 0, mWatchHandShadowColor);
        }

        private void updateStyle() {
            if (isAmbient) {
                if (analogMode) {
                    mHourPaint.setColor(Color.WHITE);
                    mMinutePaint.setColor(Color.WHITE);
                    mSecondPaint.setColor(Color.WHITE);
                    mTickAndCirclePaint.setColor(Color.WHITE);

                    mHourPaint.setAntiAlias(false);
                    mMinutePaint.setAntiAlias(false);
                    mSecondPaint.setAntiAlias(false);
                    mTickAndCirclePaint.setAntiAlias(false);

                    mHourPaint.clearShadowLayer();
                    mMinutePaint.clearShadowLayer();
                    mSecondPaint.clearShadowLayer();
                    mTickAndCirclePaint.clearShadowLayer();
                }
            } else {
                if (analogMode) {
                    mHourPaint.setColor(mWatchHandColor);
                    mMinutePaint.setColor(mWatchHandColor);
                    mSecondPaint.setColor(mWatchHandHighlightColor);
                    mTickAndCirclePaint.setColor(mWatchHandColor);

                    mHourPaint.setAntiAlias(true);
                    mMinutePaint.setAntiAlias(true);
                    mSecondPaint.setAntiAlias(true);
                    mTickAndCirclePaint.setAntiAlias(true);

                    mHourPaint.setShadowLayer(6, 0, 0, mWatchHandShadowColor);
                    mMinutePaint.setShadowLayer(6, 0, 0, mWatchHandShadowColor);
                    mSecondPaint.setShadowLayer(6, 0, 0, mWatchHandShadowColor);
                    mTickAndCirclePaint.setShadowLayer(6, 0, 0, mWatchHandShadowColor);
                }
            }
        }

    }


    /**
     * 根据 exif 信息读取旋转角度。
     *
     * @param fileName 文件名。
     * @return 旋转角度。
     */
    private int getExifRotateAngle(String fileName) {
        int angle = 0;
        try {
            ExifInterface exif = new ExifInterface(fileName);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    angle = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    angle = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    angle = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return angle;
    }

    /**
     * 计算缩放比例。
     *
     * @param options      用于获取图片大小。
     * @param targetWidth  目标宽度。
     * @param targetHeight 目标高度。
     */
    private int calculateInSample(BitmapFactory.Options options, int targetWidth, int targetHeight) {
        final int rawWidth = options.outWidth;
        final int rawHeight = options.outHeight;
        int inSample = 1;
        if (rawWidth > targetWidth || rawHeight > targetHeight) {
            final int halfWidth = rawWidth / 2;//为了避免过分压缩 例如 图片大小为 250 * 250 view 200 * 200
            final int halfHeight = rawHeight / 2;
            while ((halfWidth / inSample) >= targetWidth && (halfHeight / inSample) >= targetHeight) {
                inSample *= 2;
            }
        }
        return inSample;
    }

    /**
     * 将sp值转换为px值。
     * TextView单位为sp，Paint为px。
     */
    public static int sp2px(Context context, float spValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

}
