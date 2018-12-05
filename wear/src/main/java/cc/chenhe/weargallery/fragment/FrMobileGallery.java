package cc.chenhe.weargallery.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.bumptech.glide.request.RequestOptions;
import com.mobvoi.android.wearable.Asset;
import com.mobvoi.android.wearable.DataMap;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import cc.chenhe.lib.weartools.AssetHelper;
import cc.chenhe.lib.weartools.WTBothway;
import cc.chenhe.weargallery.BuildConfig;
import cc.chenhe.weargallery.GlideApp;
import cc.chenhe.weargallery.R;
import cc.chenhe.weargallery.activity.AtyBigImage;
import cc.chenhe.weargallery.common.CUtils;
import cc.chenhe.weargallery.common.Cc;
import cc.chenhe.weargallery.common.bean.ImageFolderBeanT;
import cc.chenhe.weargallery.ui.AlertDialog;
import cc.chenhe.weargallery.ui.recycler.CommonAdapter;
import cc.chenhe.weargallery.ui.recycler.OnItemClickListener;
import cc.chenhe.weargallery.ui.recycler.ViewHolder;
import cc.chenhe.weargallery.uilts.Logger;
import cc.chenhe.weargallery.uilts.Utils;
import ticwear.design.widget.FloatingActionButton;

public class FrMobileGallery extends FrBase {
    Context context;

    private static final int REQUEST_IMAGE = 1;
    private final String TAG = this.getClass().getSimpleName();
    private Activity activity;

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private FloatingActionButton fabRetry;
    private View ivHelper;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.activity = (Activity) context;
        this.context = context;
    }

    @Override
    protected int onGetContentViewId() {
        return R.layout.fr_gallery_list_new;
    }

    private void setVisibility(View view, boolean visible) {
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onLazyLoad() {
        super.onLazyLoad();
        ivHelper = findViewById(R.id.ivMobileGalleryHelp);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        fabRetry = findViewById(R.id.fab_retry);
        setVisibility(fabRetry, false);
        setVisibility(ivHelper, false);
        progressBar = findViewById(R.id.progress_bar);
        setVisibility(progressBar, true);

        initAdapter();
        initEvent();
        loadData(false);//只有获得读sd卡权限时才加载图片
    }

    private void initEvent() {
        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.fab_retry:
                        loadData(true);
                        fabRetry.setVisibility(View.GONE);
                        break;
                    case R.id.ivMobileGalleryHelp:
                        new AlertDialog.Builder(context)
                                .setTitle(R.string.main_retry_title)
                                .setMessage(R.string.main_retry_content)
                                .setShowPositiveButton(true)
                                .show();
                        break;
                }
            }
        };
        ivHelper.setOnClickListener(clickListener);
        fabRetry.setOnClickListener(clickListener);
    }

    /**
     * 向手机发送初始化请求
     */
    private void loadData(final boolean showTip) {
        setVisibility(fabRetry, false);
        setVisibility(ivHelper, false);
        setVisibility(progressBar, true);
        WTBothway.request(context, Cc.PATH_WATCH_INIT, String.valueOf(BuildConfig.FEATURE_VERSION), new WTBothway.BothwayCallback() {
            @Override
            public void onRespond(byte[] data) {
                if (activity.isFinishing()) return;
                JSONObject r = JSON.parseObject(new String(data));
                switch (r.getIntValue(Cc.RESULT_INIT_KEY_OPERATION_TYPE)) {
                    case Cc.RESULT_INIT_OPERATION_TYPE_CHECK_GOOGEL_LICENSE:
                        break;
                    case Cc.RESULT_INIT_OPERATION_TYPE_APP_VERSION:
                        //版本不一致
                        new AlertDialog.Builder(context)
                                .setTitle(R.string.main_load_version_conflict_title)
                                .setMessage(R.string.main_load_version_conflict_content)
                                .setShowPositiveButton(true)
                                .show();
                        showRetryLayout();
                        break;
                    case Cc.RESULT_INIT_OPERATION_TYPE_GALLERY_LIST:
                        loadImageFolders(r.getString(Cc.RESULT_INIT_KEY_DATA));
                        break;
                }
            }

            @Override
            public void onFailed(int resultCode) {
                if (showTip && !activity.isFinishing())
                    Toast.makeText(context, getString(R.string.main_load_gallery_err_toast), Toast.LENGTH_SHORT).show();
                showRetryLayout();
            }
        });
    }

    private void showRetryLayout() {
        if (!activity.isFinishing()) {
            setVisibility(fabRetry, true);
            setVisibility(ivHelper, true);
            setVisibility(progressBar, false);
        }
    }

    /**
     * 显示手机返回的相册列表
     */
    private void loadImageFolders(String r) {
        if (r.equals(Cc.GET_GALLERY_ERR_SCAN) && !activity.isFinishing()) {
            Toast.makeText(context, R.string.main_load_gallery_err_scan_toast, Toast.LENGTH_SHORT).show();
            activity.finish();
            return;
        }
        if (r.equals(Cc.GET_GALLERY_ERR_PERMISSION) && !activity.isFinishing()) {
            new AlertDialog.Builder(context)
                    .setTitle(R.string.main_load_gallery_err_permission_title)
                    .setMessage(R.string.main_load_gallery_err_permission_content)
                    .setShowPositiveButton(true)
                    .show();
            showRetryLayout();
            return;
        }
        try {
            List<ImageFolderBeanT> imageFolderBeanTs = JSON.parseArray(r, ImageFolderBeanT.class);
            setVisibility(progressBar, false);
            ((MyAdapter) recyclerView.getAdapter()).setData(imageFolderBeanTs);

        } catch (Exception e) {
            if (!activity.isFinishing()) {
                Toast.makeText(context, getString(R.string.gallery_list_parse_gallery_list_data_err), Toast.LENGTH_SHORT).show();
                activity.finish();
            }
        }
    }

    private void initAdapter() {
        MyAdapter adapter = new MyAdapter(context, new ArrayList<ImageFolderBeanT>());
        adapter.isShowHeaderView(true);
        // 添加标题 header
        TextView textView = new TextView(context);
        textView.setText(R.string.gallery_list_title);
        textView.setGravity(Gravity.CENTER);
        textView.setBackgroundColor(getResources().getColor(R.color.title_bg));
        textView.setPadding(0, (int) getResources().getDimension(R.dimen.title_padding_top)
                , 0, (int) getResources().getDimension(R.dimen.title_padding_bottom));
        adapter.addHeaderView(textView);
        adapter.setOnItemClickListener(new OnItemClickListener<ImageFolderBeanT>() {
            @Override
            public void onItemClick(ViewHolder holder, ImageFolderBeanT data, int pos) {
                AtyBigImage.start(FrMobileGallery.this, REQUEST_IMAGE, new File(data.getFirstImagePath()).getParent());
            }
        });
        adapter.setCustomFooterView(R.layout.place_holder);
        recyclerView.setAdapter(adapter);
    }

    private class MyAdapter extends CommonAdapter<ImageFolderBeanT> {

        MyAdapter(Context context, @NonNull List<ImageFolderBeanT> data) {
            super(context, data);
        }

        @Override
        public int getItemLayoutId(int viewType) {
            return R.layout.item_lv_main;
        }

        @Override
        public void bindView(final ViewHolder holder, int pos, int viewType) {
            final ImageFolderBeanT item = getItem(pos);
            holder.setText(R.id.tvName,
                    getString(R.string.ui_gallery_folder_name, item.getName(), item.getCount()));
            ((ImageView) holder.getView(R.id.ivIco)).setImageResource(R.drawable.bg_pic_default);
            holder.getView(R.id.ivIco).setTag(R.id.first_image_path, item.getFirstImagePath());

            if (!CUtils.fileIsExists(Utils.Path_cache_gallery + item.getFirstImagePath().hashCode() + ".webp")) {
                //无缩略图缓存
//                BuglyLog.i(TAG, "请求目录缩略图：" + item.getFirstImagePath());
                final WTBothway.BothwayCallback4DataMap callback = new WTBothway.BothwayCallback4DataMap() {
                    @Override
                    public void onRespond(DataMap data) {
                        if (activity.isFinishing()) return;
                        Asset asset = data.getAsset(Cc.ASSET_KEY_PIC);
                        AssetHelper.get(context, asset, new AssetHelper.AssetCallback() {
                            @Override
                            public void onResult(InputStream ins) {
                                if (ins != null && holder.getView(R.id.ivIco).getTag(R.id.first_image_path).equals(item.getFirstImagePath())) {
                                    CUtils.writeImageToDisk(CUtils.is2ByteArray(ins), Utils.Path_cache_gallery + item.getFirstImagePath().hashCode() + ".webp");
                                    GlideApp.with(context)
                                            .load(Utils.Path_cache_gallery + item.getFirstImagePath().hashCode() + ".webp")
                                            .placeholder(R.drawable.bg_pic_default)
                                            .apply(RequestOptions.circleCropTransform())
                                            .into((ImageView) holder.getView(R.id.ivIco));
                                }
                            }
                        });
                    }

                    @Override
                    public void onFailed(int resultCode) {
                        Logger.d(TAG, "缩略图加载失败：" + item.getName());
                    }
                };
                WTBothway.request(context, Cc.PATH_GET_MICRO_PICTURE, item.getFirstImagePath(), callback);
            } else {
                //有缩略图缓存
                GlideApp.with(context)
                        .load(Utils.Path_cache_gallery + item.getFirstImagePath().hashCode() + ".webp")
                        .placeholder(R.drawable.bg_pic_default)
                        .apply(RequestOptions.circleCropTransform())
                        .into((ImageView) holder.getView(R.id.ivIco));
            }

            holder.getItemView().setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    new AlertDialog.Builder(context)
                            .setTitle(R.string.ui_main_hide_gallery_title)
                            .setMessage(R.string.ui_main_hide_gallery_content)
                            .setPositiveButtonListener(new AlertDialog.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    hideGallery(new File(item.getFirstImagePath()).getParent(), holder.getAdapterPosition() - getHeaderCount());
                                }
                            })
                            .setShowNegativeButton(true)
                            .show();
                    return false;
                }
            });
        }

        private void hideGallery(String path, final int position) {
            WTBothway.request(context, Cc.PATH_HIDE_GALLERY, path, new WTBothway.BothwayCallback() {
                @Override
                public void onRespond(byte[] data) {
                    if (!activity.isFinishing()) {
                        Toast.makeText(context, getString(R.string.gallery_list_hide_ok), Toast.LENGTH_SHORT).show();
                        remove(position);
                    }
                }

                @Override
                public void onFailed(int resultCode) {
                    if (!activity.isFinishing())
                        Toast.makeText(context, getString(R.string.gallery_list_hide_fail), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
