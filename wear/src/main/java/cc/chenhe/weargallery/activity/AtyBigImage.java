package cc.chenhe.weargallery.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.GridPagerAdapter;
import android.support.wearable.view.GridViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.mobvoi.android.wearable.Asset;
import com.mobvoi.android.wearable.DataMap;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import cc.chenhe.lib.weartools.AssetHelper;
import cc.chenhe.lib.weartools.WTBothway;
import cc.chenhe.weargallery.GlideApp;
import cc.chenhe.weargallery.R;
import cc.chenhe.weargallery.bean.eventmsg.LocalImageChangedMsg;
import cc.chenhe.weargallery.bean.eventmsg.WatchfaceImageChangedMsg;
import cc.chenhe.weargallery.common.CUtils;
import cc.chenhe.weargallery.common.Cc;
import cc.chenhe.weargallery.ui.MyGridViewPager;
import cc.chenhe.weargallery.uilts.Logger;
import cc.chenhe.weargallery.uilts.Settings;
import cc.chenhe.weargallery.uilts.Utils;
import cc.chenhe.weargallery.uilts.WfSettings;

public class AtyBigImage extends WearableActivity {

    public static final String EXTRA_PARENT_PATH = "EXTRA_PARENT_PATH";

    public static final int RESULT_REFRESH_COUNT = 100;

    public static final int IS_ORIGINAL_TRUE = 1;
    public static final int IS_ORIGINAL_FALSE = 0;
    public static final int IS_ORIGINAL_LOADING = 2;

    @IntDef({IS_ORIGINAL_FALSE, IS_ORIGINAL_LOADING, IS_ORIGINAL_TRUE})
    @Retention(RetentionPolicy.SOURCE)
    private @interface HdStatus {
    }


    Context context;
    private MyGridViewPager viewPager;
    private TextView tvCount, tvMime;
    private View llTitle;
    private boolean isAutoBrightness = false;
    /*用于延时加载与顶部延时隐藏*/
    private Handler handler = new Handler();
    private Runnable animationRunnable;
    /*用于判断是否为快速滑动*/
    private long lastTime;

    private final String TAG = getClass().getSimpleName();

    public static void start(Fragment context, int reqCode, String parentPath) {
        Intent intent = new Intent(context.getContext(), AtyBigImage.class);
        intent.putExtra(EXTRA_PARENT_PATH, parentPath);
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
        context = this;
        setResult(RESULT_OK);
        initView();
        setMaxBrightness();
        initEvent();
        getFileNameList();
    }

    private void initView() {
        setContentView(R.layout.aty_big_imgae);
        viewPager = findViewById(R.id.viewPager);
        tvCount = findViewById(R.id.tvCount);
        llTitle = findViewById(R.id.llTitle);
        llTitle.setTag(false);//表示当前是不可见的
        tvMime = findViewById(R.id.tvMime);

        SharedPreferences sp = getSharedPreferences(Settings.NAME, Context.MODE_PRIVATE);
        boolean isWakeLock = sp.getBoolean(Settings.ITEM_WATCH_WEAK_LOCK, Settings.ITEM_WATCH_WEAK_LOCK_D);
        findViewById(R.id.ivWakeLock).setVisibility(isWakeLock ? View.VISIBLE : View.GONE);
        if (isWakeLock)
            setAmbientEnabled();
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

    /**
     * 请求图片文件名列表
     */
    private void getFileNameList() {
        WTBothway.request(context, Cc.PATH_GET_FILE_NAME_LIST, getIntent().getStringExtra(EXTRA_PARENT_PATH), new WTBothway.BothwayCallback() {
            @Override
            public void onRespond(byte[] data) {
                List<String> fileNames = JSON.parseArray(new String(data), String.class);
                //设置viewpager数据
                tvCount.setText(getString(R.string.big_image_count, 1, fileNames.size()));
                viewPager.setAdapter(new MyGridAdapter(fileNames));
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

            @Override
            public void onFailed(int resultCode) {
                if (!isFinishing()) {
                    Toast.makeText(context, getString(R.string.big_image_get_pics_list_err), Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });
    }

    private void initEvent() {
        viewPager.setOnPageChangeListener(new GridViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, int i1, float v, float v1, int i2, int i3) {

            }

            @Override
            public void onPageSelected(int row, int col) {
                MyGridAdapter adapter = (MyGridAdapter) viewPager.getAdapter();
                tvCount.setText(getString(R.string.big_image_count,
                        row + 1, adapter.getRowCount()));
                refreshMimeType(adapter, row, col);
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

    private void refreshMimeType(MyGridAdapter adapter, int row, int col) {
        String fileName = adapter.getItem(row).fileName;
        if ((fileName.endsWith(".gif") || fileName.endsWith(".GIF")) && col == 0) {
            tvMime.setText(R.string.big_image_gif);
            tvMime.setVisibility(View.VISIBLE);
        } else {
            tvMime.setVisibility(View.GONE);
        }
    }

    private class Item {
        Item(String fileName) {
            this.fileName = fileName;
        }

        String fileName;
        @HdStatus
        int hdStatus = IS_ORIGINAL_FALSE;
    }

    private class MyGridAdapter extends GridPagerAdapter {
        private LinkedList<View> recycledMenuViews = new LinkedList<>();
        private LinkedList<View> recycledImageViews = new LinkedList<>();
        private List<Item> data;
        /*是否正在快速滚动*/
        boolean isS = false;
        /**
         * Menu Root View id 前缀
         */
        private static final String MENU_ID_PREFIX = "666";

        MyGridAdapter(List<String> fileNames) {
            List<Item> data = new ArrayList<>();
            for (String s : fileNames) {
                data.add(new Item(s));
            }
            this.data = data;
            refreshMimeType(this, 0, 0);
        }

        /**
         * 刷新图片菜单加载高清按钮图标。
         */
        void refreshMenuView(int pos) {
            ImageView ivHd = findViewById(Integer.valueOf(MENU_ID_PREFIX + pos)).findViewById(R.id.iv_hd);
            if (ivHd == null)
                return;
            ivHd.setImageResource(data.get(pos).hdStatus != IS_ORIGINAL_TRUE ?
                    R.drawable.ic_operation_hd : R.drawable.ic_operation_watchface);
        }

        void removeItem(int position) {
            data.remove(position);
            notifyDataSetChanged();
        }

        Item getItem(int position) {
            return data.get(position);
        }

        void setHdStatus(int pos, @HdStatus int hdStatus) {
            data.get(pos).hdStatus = hdStatus;
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public int getColumnCount(int i) {
            return 2;
        }

        @SuppressLint("InflateParams")
        @Override
        public Object instantiateItem(ViewGroup viewGroup, final int position, final int c) {
            if (c == 1)
                return instantiateMenuView(viewGroup, position);
            else
                return instantiateImageView(viewGroup, position);
        }

        private View instantiateMenuView(ViewGroup viewGroup, final int position) {
            View rootView;
            if (recycledMenuViews.size() > 0) {
                rootView = recycledMenuViews.getFirst();
                recycledMenuViews.removeFirst();
            } else {
                rootView = LayoutInflater.from(context)
                        .inflate(R.layout.view_big_image_operation, viewGroup, false);
            }
            rootView.setId(Integer.valueOf(MENU_ID_PREFIX + position));
            viewGroup.addView(rootView);
            View.OnClickListener listener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    switch (view.getId()) {
                        case R.id.iv_hd:
                            if (data.get(position).hdStatus == IS_ORIGINAL_TRUE)
                                actionSetWatchface(position);
                            else
                                actionLoadHD(position);
                            viewPager.setCurrentItem(position, 0);
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
            rootView.findViewById(R.id.iv_hd).setOnClickListener(listener);
            rootView.findViewById(R.id.iv_del).setOnClickListener(listener);
            View view;
            view = rootView.findViewById(R.id.iv_rotate);
            if (view != null) view.setOnClickListener(listener);
            view = rootView.findViewById(R.id.iv_rotate_left);
            if (view != null) view.setOnClickListener(listener);
            view = rootView.findViewById(R.id.iv_rotate_right);
            if (view != null) view.setOnClickListener(listener);

            ImageView hd = rootView.findViewById(R.id.iv_hd);
            if (data.get(position).hdStatus != IS_ORIGINAL_TRUE) {
                // 还未加载完成高清图
                hd.setImageResource(R.drawable.ic_operation_hd);
            } else {
                hd.setImageResource(R.drawable.ic_operation_watchface);
            }
            return rootView;
        }

        private View instantiateImageView(ViewGroup viewGroup, final int position) {
            final View view;
            if (recycledImageViews.size() > 0) {
                view = recycledImageViews.getFirst();
                recycledImageViews.removeFirst();
            } else {
                LayoutInflater inflater = LayoutInflater.from(AtyBigImage.this);
                view = inflater.inflate(R.layout.aty_big_image_item, viewGroup, false);
                ((SubsamplingScaleImageView) view.findViewById(R.id.subImageView))
                        .setOrientation(SubsamplingScaleImageView.ORIENTATION_USE_EXIF);
            }
            view.setId(position);
            view.setTag(R.id.photo_view_tag_is_original, IS_ORIGINAL_FALSE);
            final SubsamplingScaleImageView subImageView = view.findViewById(R.id.subImageView);
            subImageView.setDoubleTapZoomDpi(Utils.IMAGE_VIEW_MAX_DPI);
            subImageView.setDoubleTapZoomDpi1(Utils.IMAGE_VIEW_MID_DPI);
            ImageView imageView = view.findViewById(R.id.imageView);
            if (imageView != null)
                imageView.setVisibility(View.GONE);
            subImageView.setVisibility(View.VISIBLE);
            viewGroup.addView(view);

            if (isS) {
                // 正在快速滑动
                subImageView.setImage(ImageSource.resource(R.drawable.bg_pic_default));
                return view;
            }

            //图片在手机上的路径
            final String filePath = getIntent().getStringExtra(EXTRA_PARENT_PATH) + "/" + data.get(position).fileName;
            subImageView.setOrientation(SubsamplingScaleImageView.ORIENTATION_USE_EXIF);
            subImageView.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE);

            if (data.get(position).hdStatus == IS_ORIGINAL_TRUE) {
                // 已经加载过高清图
                final String wearFilePath = Utils.Path_cache_original + filePath.hashCode() +
                        "." + CUtils.getExtraName(filePath);
                setOriginalImage(view, wearFilePath);
                return view;
            }

            //显示默认图片
            subImageView.setImage(ImageSource.resource(R.drawable.bg_pic_default));
            if (!CUtils.fileIsExists(Utils.Path_cache_single + filePath.hashCode() + ".webp")) {
                //无缩略图缓存
                Logger.d(TAG, "请求单张缩略图：" + filePath);
                view.setTag(R.id.file_path, filePath);
                WTBothway.BothwayCallback4DataMap callback = new WTBothway.BothwayCallback4DataMap() {
                    @Override
                    public void onRespond(final DataMap data) {
                        Asset asset = data.getAsset(Cc.ASSET_KEY_PIC);
                        AssetHelper.get(context, asset, new AssetHelper.AssetCallback() {
                            @Override
                            public void onResult(InputStream ins) {
                                if (ins == null) return;
                                CUtils.writeImageToDisk(CUtils.is2ByteArray(ins), Utils.Path_cache_single + filePath.hashCode() + ".webp");
                                if (isFinishing()) return;
                                if (filePath.equals(view.getTag(R.id.file_path))) {
                                    subImageView.setImage(ImageSource.uri(Utils.Path_cache_single + filePath.hashCode() + ".webp"));
                                }
                            }
                        });
                    }

                    @Override
                    public void onFailed(int resultCode) {
                        if (!isFinishing())
                            Toast.makeText(context, getString(R.string.big_image_load_micro_picture_err, data.get(position).fileName), Toast.LENGTH_SHORT).show();
                    }
                };
                WTBothway.request(context, Cc.PATH_GET_SINGLE_PICTURE, filePath, callback);
            } else {
                //有缩略图缓存
                subImageView.setImage(ImageSource.uri(Utils.Path_cache_single + filePath.hashCode() + ".webp"));
            }
            return view;
        }

        @Override
        public void destroyItem(ViewGroup viewGroup, int row, int c, Object o) {
            viewGroup.removeView((View) o);
            if (c == 1) {
                recycledMenuViews.addLast((View) o);
            } else if (c == 0) {
                recycledImageViews.addLast((View) o);
            }
        }

        @Override
        public boolean isViewFromObject(View view, Object o) {
            return view == o;
        }
    }

    /**
     * 设置为表盘，
     */
    private void actionSetWatchface(final int position) {
        final MyGridAdapter adapter = (MyGridAdapter) viewPager.getAdapter();
        if (position == -1 || adapter == null)
            return;
        final String mobileFilePath = getIntent().getStringExtra(EXTRA_PARENT_PATH) + "/" +
                adapter.getItem(position).fileName;
        final String wearFilePath = Utils.Path_cache_original + mobileFilePath.hashCode() +
                "." + CUtils.getExtraName(mobileFilePath);
        WfSettings.setImage(context, wearFilePath);
        EventBus.getDefault().post(new WatchfaceImageChangedMsg(wearFilePath));
        if (Settings.showSwitchWfTip(context))
            AtySwitchGuide.start(context);
    }

    /**
     * 加载高清图片
     */
    private void actionLoadHD(final int position) {
        final MyGridAdapter adapter = (MyGridAdapter) viewPager.getAdapter();
        final View view = viewPager.findViewById(position);
        if (position == -1 || adapter == null || view == null)
            return;
        if (adapter.getItem(position).hdStatus != IS_ORIGINAL_FALSE)
            return;

        adapter.setHdStatus(position, IS_ORIGINAL_LOADING);
        final String mobileFilePath = getIntent().getStringExtra(EXTRA_PARENT_PATH) + "/" +
                adapter.getItem(position).fileName;
        final String wearFilePath = Utils.Path_cache_original + mobileFilePath.hashCode() +
                "." + CUtils.getExtraName(mobileFilePath);
        if (!CUtils.fileIsExists(wearFilePath)) {
            WTBothway.BothwayCallback4DataMap callback = new WTBothway.BothwayCallback4DataMap() {
                @Override
                public void onRespond(final DataMap data) {
                    Asset asset = data.getAsset(Cc.ASSET_KEY_PIC);
                    AssetHelper.get(context, asset, new AssetHelper.AssetCallback() {
                        @Override
                        public void onResult(InputStream ins) {
                            if (ins == null) return;
                            Logger.d(TAG, "保存原图:" + wearFilePath);
                            CUtils.writeImageToDisk(ins, wearFilePath);
                            EventBus.getDefault().post(new LocalImageChangedMsg());
                            if (isFinishing()) return;
                            setOriginalImage(view, wearFilePath);
                            Toast.makeText(context, getString(R.string.big_image_load_hd_ok), Toast.LENGTH_SHORT).show();
                            adapter.setHdStatus(position, IS_ORIGINAL_TRUE);
                            adapter.refreshMenuView(position);
                        }
                    });
                }

                @Override
                public void onFailed(int resultCode) {
                    if (isFinishing()) return;
                    Toast.makeText(context, getString(R.string.big_image_load_hd_err), Toast.LENGTH_SHORT).show();
                    adapter.setHdStatus(position, IS_ORIGINAL_FALSE);
                }
            };
            Toast.makeText(context, getString(R.string.big_image_load_hd_ing), Toast.LENGTH_SHORT).show();
            Logger.d(TAG, "请求高清图：" + mobileFilePath);
            WTBothway.request(context, Cc.PATH_GET_ORIGINAL_PICTURE, mobileFilePath.getBytes(), 1000 * 25, callback);
        } else {
            Logger.d(TAG, "缓存读取高清图：" + mobileFilePath);
            setOriginalImage(view, wearFilePath);
            adapter.setHdStatus(position, IS_ORIGINAL_TRUE);
            adapter.refreshMenuView(position);
            Toast.makeText(context, getString(R.string.big_image_load_hd_ok), Toast.LENGTH_SHORT).show();
        }
    }

    private void setOriginalImage(@NonNull View itemRootView, @NonNull String imageFileName) {
        SubsamplingScaleImageView subImageView = itemRootView.findViewById(R.id.subImageView);
        ImageView imageView = itemRootView.findViewById(R.id.imageView);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imageFileName, options);
        if ("image/gif".equals(options.outMimeType)) {
            subImageView.setVisibility(View.GONE);
            if (imageView == null) {
                ViewStub vs = itemRootView.findViewById(R.id.viewStubImageView);
                vs.inflate();
                imageView = itemRootView.findViewById(R.id.imageView);
            } else {
                imageView.setVisibility(View.VISIBLE);
            }
            GlideApp.with(AtyBigImage.this)
                    .load(imageFileName)
                    .into(imageView);
        } else {
            if (imageView != null)
                imageView.setVisibility(View.GONE);
            subImageView.setImage(ImageSource.uri(imageFileName));
            subImageView.setVisibility(View.VISIBLE);
        }
    }


    /**
     * 旋转图片
     */
    private void actionRotate(int position, int d) {
        View view = viewPager.findViewById(position);
        view.setRotation(view.getRotation() + d);
    }

    /**
     * 删除图片
     */
    private void actionDelete(final int position) {
        final MyGridAdapter adapter = (MyGridAdapter) viewPager.getAdapter();
        if (adapter == null || position > adapter.getRowCount() - 1)
            return;

        final String phoneFilePath = getIntent().getStringExtra(EXTRA_PARENT_PATH) + "/" + adapter.getItem(position).fileName;
        WTBothway.BothwayCallback callback = new WTBothway.BothwayCallback() {
            @Override
            public void onRespond(byte[] data) {
                int r = Integer.valueOf(new String(data));
                switch (r) {
                    case Cc.RESULT_DEL_PHONE_PICTURE_OK:
                        Toast.makeText(context, getString(R.string.big_image_del_phone_picture_ok), Toast.LENGTH_SHORT).show();
                        setResult(RESULT_REFRESH_COUNT);
                        break;
                    case Cc.RESULT_DEL_PHONE_PICTURE_FAILED:
                        Toast.makeText(context, getString(R.string.big_image_del_phone_picture_err), Toast.LENGTH_SHORT).show();
                        break;
                    case Cc.RESULT_DEL_PHONE_PICTURE_NO_PERMISSION:
                        Toast.makeText(context, getString(R.string.big_image_del_phone_picture_no_permission), Toast.LENGTH_SHORT).show();
                        break;
                }
                if (r == Cc.RESULT_DEL_PHONE_PICTURE_OK) {
                    CUtils.deleteFile(new File(Utils.Path_cache_single + phoneFilePath.hashCode() + "." + CUtils.getExtraName(phoneFilePath)));
                    adapter.removeItem(position);
                    tvCount.setText(getString(R.string.big_image_count,
                            position + 1, viewPager.getAdapter().getRowCount()));
                    if (adapter.getRowCount() <= 0) {
                        finish();
                        return;
                    }
                    refreshMimeType(adapter, position, 0);
                }
            }

            @Override
            public void onFailed(int resultCode) {
                Toast.makeText(context, getString(R.string.big_image_del_phone_picture_time_out), Toast.LENGTH_SHORT).show();
            }
        };
        WTBothway.request(context, Cc.PATH_DEL_PHONE_PICTURE, phoneFilePath, callback);
    }
}
