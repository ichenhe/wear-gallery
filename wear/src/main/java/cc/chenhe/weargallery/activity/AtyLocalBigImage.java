package cc.chenhe.weargallery.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import androidx.annotation.IntDef;
import androidx.fragment.app.Fragment;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.GridPagerAdapter;
import android.support.wearable.view.GridViewPager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.LinkedList;
import java.util.List;

import cc.chenhe.weargallery.GlideApp;
import cc.chenhe.weargallery.R;
import cc.chenhe.weargallery.bean.eventmsg.WatchfaceImageChangedMsg;
import cc.chenhe.weargallery.common.CUtils;
import cc.chenhe.weargallery.ui.MyGridViewPager;
import cc.chenhe.weargallery.uilts.ImageUtils;
import cc.chenhe.weargallery.uilts.Settings;
import cc.chenhe.weargallery.uilts.Utils;
import cc.chenhe.weargallery.uilts.WfSettings;

public class AtyLocalBigImage extends WearableActivity {
    public static final int TYPE_MY_IMAGES = 0;//腕间图库hd缓存目录
    public static final int TYPE_OTHER_IMAGES = 1;//其他图片目录
    public static final int RESULT_REFRESH_COUNT = 100;
    public static final String RESULT_EXTRA_COUNT = "count";
    public static final String RESULT_EXTRA_PARENT_PATH = "parent_path";

    @IntDef({TYPE_MY_IMAGES, TYPE_OTHER_IMAGES})
    @Retention(RetentionPolicy.SOURCE)
    @interface LocalImageType {
    }

    private static final String EXTRA_PARENT_PATH = "parent_path";
    private static final String EXTRA_TYPE = "type";

    Context context;
    private String parentPath;
    private int type;

    private MyGridViewPager viewPager;
    private TextView tvCount;
    private View llTitle;
    private boolean isAutoBrightness = false;
    /*用于延时加载与顶部延时隐藏*/
    private Handler handler = new Handler();
    private Runnable animationRunnable;
    /*用于判断是否为快速滑动*/
    private long lastTime;

    public static void start(Fragment context, int reqCode, String parentPath, @LocalImageType int type) {
        Intent intent = new Intent(context.getContext(), AtyLocalBigImage.class);
        intent.putExtra(EXTRA_PARENT_PATH, parentPath);
        intent.putExtra(EXTRA_TYPE, type);
        context.startActivityForResult(intent, reqCode);
    }

    @Override
    protected void onDestroy() {
        if (isAutoBrightness)
            Utils.startAutoBrightness(context);
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.context = this;
        setResult(RESULT_OK);
        parentPath = getIntent().getStringExtra(EXTRA_PARENT_PATH);
        type = getIntent().getIntExtra(EXTRA_TYPE, -1);
        if (TextUtils.isEmpty(parentPath) || type == -1)
            return;
        initView();
        setMaxBrightness();
        initData();
        initEvent();
    }

    private void initView() {
        setContentView(R.layout.aty_big_imgae);
        viewPager = findViewById(R.id.viewPager);
        tvCount = findViewById(R.id.tvCount);
        llTitle = findViewById(R.id.llTitle);
        llTitle.setTag(false);//表示当前是不可见的
        findViewById(R.id.tvMime).setVisibility(View.GONE);

        SharedPreferences sp = context.getSharedPreferences(Settings.NAME, Context.MODE_PRIVATE);
        boolean isWakeLock = sp.getBoolean(Settings.ITEM_WATCH_WEAK_LOCK, Settings.ITEM_WATCH_WEAK_LOCK_D);
        findViewById(R.id.ivWakeLock).setVisibility(isWakeLock ? View.VISIBLE : View.GONE);
        if (isWakeLock)
            setAmbientEnabled();
    }

    private void initData() {
        List<String> mFileNames;
        mFileNames = ImageUtils.getChildPicturesName(parentPath);
        if (mFileNames == null)
            return;
        tvCount.setText(getString(R.string.big_image_count, 1, mFileNames.size()));
        //设置viewpager数据
        viewPager.setAdapter(new MyGridAdapter(mFileNames));
        //标题延时渐隐
        animationRunnable = new Runnable() {
            @Override
            public void run() {
                AlphaAnimation mHideAnimation = new AlphaAnimation(1.0f, 0.0f);
                mHideAnimation.setDuration(400);
                mHideAnimation.setFillAfter(true);
                llTitle.startAnimation(mHideAnimation);
            }
        };
        handler.postDelayed(animationRunnable, 1000);
    }

    /**
     * 设置锁定最大亮度
     */
    private void setMaxBrightness() {
        if (getSharedPreferences(Settings.NAME, MODE_PRIVATE)
                .getBoolean(Settings.ITEM_MAX_BRIGHTNESS, Settings.ITEM_MAX_BRIGHTNESS_D)) {
            if (Build.VERSION.SDK_INT >= 23 && android.provider.Settings.System.canWrite(context)) {
                isAutoBrightness = Utils.IsAutoBrightness(context);
                Utils.setCurWindowBrightness(context, 255);
            }
        }
    }

    private void initEvent() {

        viewPager.setOnPageChangeListener(new GridViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, int i1, float v, float v1, int i2, int i3) {

            }

            @Override
            public void onPageSelected(int row, int col) {
                //更新标题
                tvCount.setText(getString(R.string.big_image_count,
                        row + 1, viewPager.getAdapter().getRowCount()));
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                switch (state) {
                    case GridViewPager.SCROLL_STATE_DRAGGING:
                        handler.removeCallbacks(animationRunnable);
                        if (System.currentTimeMillis() - lastTime <= 400) {
                            ((MyGridAdapter) viewPager.getAdapter()).isS = true;
                        } else {
                            //标题渐显
                            if (!(Boolean) llTitle.getTag()) {
                                AlphaAnimation mShowAnimation = new AlphaAnimation(0.0f, 1.0f);
                                mShowAnimation.setDuration(400);
                                mShowAnimation.setFillAfter(true);
                                llTitle.setTag(true);
                                llTitle.startAnimation(mShowAnimation);
                            }
                        }
                        lastTime = System.currentTimeMillis();
                        break;
                    case GridViewPager.SCROLL_STATE_IDLE:
                        if (((MyGridAdapter) viewPager.getAdapter()).isS) {
                            animationRunnable = new Runnable() {
                                @Override
                                public void run() {
                                    ((MyGridAdapter) viewPager.getAdapter()).isS = false;
                                    viewPager.getAdapter().notifyDataSetChanged();

                                    //标题延时渐隐
                                    animationRunnable = new Runnable() {
                                        @Override
                                        public void run() {
                                            AlphaAnimation mHideAnimation = new AlphaAnimation(1.0f, 0.0f);
                                            mHideAnimation.setDuration(400);
                                            mHideAnimation.setFillAfter(true);
                                            llTitle.setTag(false);
                                            llTitle.startAnimation(mHideAnimation);
                                        }
                                    };
                                    handler.postDelayed(animationRunnable, 600);
                                }
                            };
                            handler.postDelayed(animationRunnable, 400);
                        } else {
                            //标题延时渐隐
                            animationRunnable = new Runnable() {
                                @Override
                                public void run() {
                                    AlphaAnimation mHideAnimation = new AlphaAnimation(1.0f, 0.0f);
                                    mHideAnimation.setDuration(400);
                                    mHideAnimation.setFillAfter(true);
                                    llTitle.setTag(false);
                                    llTitle.startAnimation(mHideAnimation);
                                }
                            };
                            handler.postDelayed(animationRunnable, 600);
                        }
                        break;
                }
            }
        });
    }

    private void requireRefreshImageCount(int count) {
        Intent data = new Intent();
        data.putExtra(RESULT_EXTRA_COUNT, count);
        data.putExtra(RESULT_EXTRA_PARENT_PATH, parentPath);
        setResult(RESULT_REFRESH_COUNT, data);
    }

    private class MyGridAdapter extends GridPagerAdapter {
        private LinkedList<SubsamplingScaleImageView> recycledViews = new LinkedList<>();
        /*缩小显示未加载时默认图片*/
        private BitmapFactory.Options opts;
        private List<String> fileNames;
        /*是否正在快速滚动*/
        boolean isS = false;

        MyGridAdapter(List<String> fileNames) {
            this.fileNames = fileNames;
            opts = new BitmapFactory.Options();
            opts.inSampleSize = 2;
        }

        List<String> getFileNames() {
            return fileNames;
        }

        void removeItem(int position) {
            fileNames.remove(position);
            notifyDataSetChanged();
        }

        @Override
        public int getRowCount() {
            return fileNames.size();
        }

        @Override
        public int getColumnCount(int i) {
            return 2;
        }

        @SuppressLint("InflateParams")
        @Override
        public Object instantiateItem(ViewGroup viewGroup, final int position, final int c) {
            if (c == 1) {
                /*第一页：操作按钮*/
                View rootView;
                rootView = LayoutInflater.from(context).inflate(R.layout.view_big_image_operation, null);
                viewGroup.addView(rootView);
                View.OnClickListener listener = new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        switch (view.getId()) {
                            case R.id.iv_hd:
                                String fileName = parentPath + File.separator + fileNames.get(position);
                                WfSettings.setImage(context, fileName);
                                EventBus.getDefault().post(new WatchfaceImageChangedMsg(fileName));
                                viewPager.setCurrentItem(position, 0);
                                if (Settings.showSwitchWfTip(context))
                                    AtySwitchGuide.start(context);
                                break;
                            case R.id.iv_rotate:
                                actionRotate(position, -90);
                                break;
                            case R.id.iv_rotate_left:
                                actionRotate(position, -90);
                                break;
                            case R.id.iv_rotate_right:
                                actionRotate(position, 90);
                                break;
                            case R.id.iv_del:
                                viewPager.setCurrentItem(position, 0);
                                actionDelete(position);
                                break;
                        }
                    }
                };
                ImageView ivHd = rootView.findViewById(R.id.iv_hd);
                ivHd.setImageResource(R.drawable.ic_operation_watchface);
                ivHd.setOnClickListener(listener);
                rootView.findViewById(R.id.iv_del).setOnClickListener(listener);
                View view;
                view = rootView.findViewById(R.id.iv_rotate);
                if (view != null) view.setOnClickListener(listener);
                view = rootView.findViewById(R.id.iv_rotate_left);
                if (view != null) view.setOnClickListener(listener);
                view = rootView.findViewById(R.id.iv_rotate_right);
                if (view != null) view.setOnClickListener(listener);
                return rootView;
            }
            /*第二页：显示图片*/

            // 判断 Gif
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            String fileName = parentPath + File.separator + fileNames.get(position);
            BitmapFactory.decodeFile(fileName, options);
            if ("image/gif".equals(options.outMimeType)) {
                ImageView imageView = new ImageView(context);
                imageView.setId(position);
                imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                GlideApp.with(AtyLocalBigImage.this)
                        .load(fileName)
                        .into(imageView);
                viewGroup.addView(imageView);
                return imageView;
            } else {
                // view 重用
                SubsamplingScaleImageView imageView;
                if (recycledViews.size() > 0) {
                    imageView = recycledViews.getFirst();
                    recycledViews.removeFirst();
                } else {
                    imageView = new SubsamplingScaleImageView(context);
                    imageView.setOrientation(SubsamplingScaleImageView.ORIENTATION_USE_EXIF);
                    imageView.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE);
                }
                imageView.setId(position);
                imageView.setImage(ImageSource.uri(fileName));
                imageView.setDoubleTapZoomDpi(Utils.IMAGE_VIEW_MAX_DPI);
                imageView.setDoubleTapZoomDpi1(Utils.IMAGE_VIEW_MID_DPI);
                viewGroup.addView(imageView);
                return imageView;
            }
        }

        @Override
        public void destroyItem(ViewGroup viewGroup, int row, int column, Object o) {
            viewGroup.removeView((View) o);
            if (column == 0 && o instanceof SubsamplingScaleImageView)
                recycledViews.addLast((SubsamplingScaleImageView) o);
        }

        @Override
        public boolean isViewFromObject(View view, Object o) {
            return view == o;
        }
    }

    /**
     * 旋转图片
     */
    private void actionRotate(int position, int d) {
        View imageView = viewPager.findViewById(position);
        imageView.setRotation(imageView.getRotation() + d);
    }

    /**
     * 删除图片
     */
    private void actionDelete(final int position) {
        MyGridAdapter adapter = (MyGridAdapter) viewPager.getAdapter();
        if (position > adapter.getRowCount() - 1)
            return;
        boolean r;
        if (type == TYPE_MY_IMAGES) {
            //删除文件
            r = CUtils.deleteFile(new File(parentPath, adapter.getFileNames().get(position)));
        } else {
            //删除媒体库
            String params[] = new String[]{parentPath + File.separator + adapter.getFileNames().get(position)};
            r = 1 == context.getContentResolver().delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaStore.Images.Media.DATA + " = ?", params);
        }
        if (r) {
            Toast.makeText(context, R.string.big_image_del_phone_picture_ok, Toast.LENGTH_SHORT).show();
            adapter.removeItem(position);
            int newPos = Math.min(position + 1, adapter.getRowCount());
            tvCount.setText(getString(R.string.big_image_count, newPos, adapter.getRowCount()));
            requireRefreshImageCount(adapter.getRowCount());
            if (adapter.getRowCount() <= 0) {
                finish();
            }
        } else {
            Toast.makeText(context, R.string.big_image_del_phone_picture_err, Toast.LENGTH_SHORT).show();
        }
    }
}
