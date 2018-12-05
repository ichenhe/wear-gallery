package cc.chenhe.weargallery;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import cc.chenhe.weargallery.bean.ImageFolderBean;
import cc.chenhe.weargallery.utils.ImageSelectObservable;

/**
 * Created by 晨鹤 on 2016/12/3.
 */

public class AtyPreviewPics extends AtyBase implements View.OnClickListener {
    /**
     * 显示/隐藏 过程持续时间
     */
    private static final int SHOW_HIDE_CONTROL_ANIMATION_TIME = 300;
    private ViewPagerFixed mPhotoPager;

    /**
     * 标题栏
     */
    private Toolbar toolbar;

    /**
     * 选择按钮
     */
    private TextView mCheckedTv;
    /**
     * 控制显示、隐藏顶部标题栏
     */
    private boolean isHeadViewShow = true;
    private View mFooterView;

    /**
     * 需要预览的所有图片
     */
    private List<ImageFolderBean> mAllImage;
    /**
     * x选择的所有图片
     */
    private List<ImageFolderBean> mSelectImage;

    private Context context;

    /**
     * 预览文件夹下所有图片
     *
     * @param activity    Activity
     * @param position    position 当前显示位置
     * @param requestCode requestCode
     */
    public static void startPreviewPhotoActivityForResult(Activity activity, int position, int requestCode, boolean isSelectMode) {
        Intent intent = new Intent(activity, AtyPreviewPics.class);
        intent.putExtra("position", position);
        intent.putExtra("isSelectMode", isSelectMode);
        activity.startActivityForResult(intent, requestCode);
        activity.overridePendingTransition(R.anim.common_scale_small_to_large, 0);
    }

    /**
     * 预览选择的图片
     *
     * @param activity    Activity
     * @param requestCode requestCode
     */
    public static void startPreviewActivity(Activity activity, int requestCode) {
        Intent intent = new Intent(activity, AtyPreviewPics.class);
        intent.putExtra("preview", true);
        intent.putExtra("isSelectMode", true);
        activity.startActivityForResult(intent, requestCode);
        activity.overridePendingTransition(R.anim.common_scale_small_to_large, 0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.aty_preview_pics);

        initImages();
        initView();
        initAdapter();

        /*全屏*/
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }
    }

    /**
     * 初始化图片数组
     */
    private void initImages() {
        mAllImage = new ArrayList<>();
        mSelectImage = ImageSelectObservable.getInstance().getSelectImages();

        if (getIntent().getBooleanExtra("preview", false)) {
            mAllImage.addAll(ImageSelectObservable.getInstance().getSelectImages());
        } else {
            mAllImage.addAll(ImageSelectObservable.getInstance().getFolderAllImages());
        }
    }

    /**
     * 初始化控件
     */
    private void initView() {
        /*标题栏*/
        String title = getIntent().getIntExtra("position", 0) + 1 + "/" + mAllImage.size();

        toolbar = findViewById(R.id.bar);
        toolbar.setTitle(title);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        /*底部菜单栏*/
        mFooterView = findViewById(R.id.rl_check);
        if (getIntent().getBooleanExtra("isSelectMode", false))
            mFooterView.setVisibility(View.VISIBLE);
        else
            mFooterView.setVisibility(View.GONE);

        mCheckedTv = findViewById(R.id.ctv_check);
        mCheckedTv.setEnabled(mSelectImage.contains(mAllImage.get(getIntent().getIntExtra("position", 0))));
        mFooterView.setOnClickListener(this);

        mPhotoPager = findViewById(R.id.vp_preview);
    }

    /**
     * 更新选择的顺序
     */
    private void subSelectPosition() {
        for (int index = 0, len = mSelectImage.size(); index < len; index++) {
            ImageFolderBean folderBean = mSelectImage.get(index);
            folderBean.selectPosition = index + 1;
        }
    }

    /**
     * adapter的初始化
     */
    private void initAdapter() {
        mPhotoPager = findViewById(R.id.vp_preview);
        PreviewAdapter previewAdapter = new PreviewAdapter(mAllImage);
        mPhotoPager.setAdapter(previewAdapter);
        mPhotoPager.setPageMargin(5);
        mPhotoPager.setCurrentItem(getIntent().getIntExtra("position", 0));

        mPhotoPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageSelected(int arg0) {
                String text = (arg0 + 1) + "/" + mAllImage.size();
                toolbar.setTitle(text);
                mCheckedTv.setEnabled(mSelectImage.contains(mAllImage.get(arg0)));
            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {

            }

            @Override
            public void onPageScrollStateChanged(int arg0) {

            }
        });

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.rl_check:
                addOrRemoveImage();
                break;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        ImageSelectObservable.getInstance().updateImageSelectChanged();
        overridePendingTransition(0, R.anim.common_scale_large_to_small);
    }

    /**
     * 添加或者删除当前操作的图片
     */
    private void addOrRemoveImage() {
        ImageFolderBean imageBean = mAllImage.get(mPhotoPager.getCurrentItem());

        if (mSelectImage.contains(imageBean)) {
            mSelectImage.remove(imageBean);
            subSelectPosition();
            mCheckedTv.setEnabled(false);
        } else {
            mSelectImage.add(imageBean);
            imageBean.selectPosition = mSelectImage.size();
            mCheckedTv.setEnabled(true);
        }
    }

    /**
     * 显示顶部，底部view动画
     */
    private void showControls() {
        AlphaAnimation animation = new AlphaAnimation(0f, 1f);
        animation.setFillAfter(true);
        animation.setDuration(SHOW_HIDE_CONTROL_ANIMATION_TIME);
        isHeadViewShow = true;
        toolbar.startAnimation(animation);
        toolbar.setVisibility(View.VISIBLE);
        if (getIntent().getBooleanExtra("isSelectMode", false)) {
            mFooterView.startAnimation(animation);
            mFooterView.setVisibility(View.VISIBLE);
        }

        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(0);
    }

    /**
     * 隐藏顶部，底部view 动画
     */
    private void hideControls() {
        AlphaAnimation animation = new AlphaAnimation(1f, 0f);
        animation.setFillAfter(true);
        animation.setDuration(SHOW_HIDE_CONTROL_ANIMATION_TIME);
        isHeadViewShow = false;
        toolbar.startAnimation(animation);

        mFooterView.startAnimation(animation);

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
    }

    /**
     * 简单的适配器
     */
    class PreviewAdapter extends PagerAdapter {
        private List<ImageFolderBean> photos;
        private LinkedList<View> recycledViews = new LinkedList<>();

        PreviewAdapter(List<ImageFolderBean> photoList) {
            super();
            this.photos = photoList;
        }

        @Override
        public int getCount() {
            return photos.size();
        }

        @Override
        public boolean isViewFromObject(@NonNull View arg0, @NonNull Object arg1) {
            return arg0 == arg1;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View) object);
            recycledViews.addLast((View) object);
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, final int position) {
            View view;
            // View 回收重用
            if (recycledViews.size() > 0) {
                view = recycledViews.getFirst();
                recycledViews.removeFirst();
            } else {
                LayoutInflater inflater = LayoutInflater.from(AtyPreviewPics.this);
                view = inflater.inflate(R.layout.vp_item_preview_pics, container, false);
            }

            View.OnClickListener onClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isHeadViewShow) {
                        hideControls();
                    } else {
                        showControls();
                    }
                }
            };

            SubsamplingScaleImageView subImageView = view.findViewById(R.id.subImageView);
            ImageView imageView = view.findViewById(R.id.imageView);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(photos.get(position).path, options);
            if ("image/gif".equals(options.outMimeType)) {
                subImageView.setVisibility(View.GONE);
                if (imageView == null) {
                    ViewStub vs = view.findViewById(R.id.viewStubImageView);
                    vs.inflate();
                    imageView = view.findViewById(R.id.imageView);
                } else {
                    imageView.setVisibility(View.VISIBLE);
                }
                imageView.setOnClickListener(onClickListener);
                GlideApp.with(context)
                        .load(new File(photos.get(position).path))
                        .into(imageView);
            } else {
                if (imageView != null)
                    imageView.setVisibility(View.GONE);
                subImageView.setVisibility(View.VISIBLE);
                subImageView.setOnClickListener(onClickListener);
                subImageView.setImage(ImageSource.uri(photos.get(position).path));
                subImageView.setOrientation(SubsamplingScaleImageView.ORIENTATION_USE_EXIF);
            }
            container.addView(view);
            return view;
        }
    }
}
