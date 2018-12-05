package cc.chenhe.weargallery.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cc.chenhe.weargallery.GlideApp;
import cc.chenhe.weargallery.R;
import cc.chenhe.weargallery.activity.AtyLocalBigImage;
import cc.chenhe.weargallery.bean.eventmsg.LocalImageChangedMsg;
import cc.chenhe.weargallery.common.bean.ImageFolderBeanT;
import cc.chenhe.weargallery.ui.recycler.CommonAdapter;
import cc.chenhe.weargallery.ui.recycler.OnItemClickListener;
import cc.chenhe.weargallery.ui.recycler.ViewHolder;
import cc.chenhe.weargallery.uilts.ImageUtils;
import cc.chenhe.weargallery.uilts.Utils;

/**
 * 显示本地相册列表，包含接收的图片和其他app图片。
 */
public class FrLocalGallery extends FrBase {

    /*图片扫描完成后传回的msg.what*/
    private static final int MSG_LOAD_PICS_FOLDERS = 10;
    private static final int REQUEST_MY_IMAGE = 0;
    private static final int REQUEST_OTHER_IMAGE = 1;

    Context context;
    Activity activity;

    /*扫描图片回调*/
    private MyHandler handler;
    /*相册列表,与adapter数据同步*/
    ArrayList<ImageFolderBeanT> imageFolderList;

    boolean needRefresh;
    boolean loading = false;

    private RecyclerView recyclerView;

    @Subscribe()
    public void onLocalGalleryChanged(LocalImageChangedMsg msg) {
        if (getUserVisibleHint() && !loading)
            loadData();
        else
            needRefresh = true;
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
    }

    @Override
    protected int onGetContentViewId() {
        return R.layout.fr_local_gallery_list_new;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        this.activity = (Activity) context;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            if (needRefresh && !loading)
                loadData();
        }
    }

    @Override
    protected void onLazyLoad() {
        super.onLazyLoad();
        imageFolderList = new ArrayList<>();
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.addItemDecoration(new SpaceItemDecoration(20));

        //检查权限
        if (Build.VERSION.SDK_INT >= 23 && context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
            return;
        }
        loadData();
    }

    private void loadData() {
        if (handler == null)
            handler = new MyHandler(this);
        loading = true;
        needRefresh = false;
        ImageUtils.loadLocalFolderContainsImage(context, handler, MSG_LOAD_PICS_FOLDERS);
    }

    private static class MyHandler extends Handler {
        private WeakReference<FrLocalGallery> wr;

        MyHandler(FrLocalGallery fr) {
            wr = new WeakReference<>(fr);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what != MSG_LOAD_PICS_FOLDERS)
                return;
            FrLocalGallery fr = wr.get();
            if (fr == null || fr.activity.isFinishing())
                return;
            fr.loading = false;
            //图片目录扫描完成
            fr.imageFolderList.clear();
            if (msg.obj != null) {
                fr.imageFolderList.addAll((Collection<? extends ImageFolderBeanT>) msg.obj);
            }
            fr.imageFolderList.add(0, new ImageFolderBeanT(Utils.Path_cache_original
                    .substring(0, Utils.Path_cache_original.length() - 1), null,
                    "", ImageUtils.getChildPicturesCount(Utils.Path_cache_original)));
            fr.initAdapter(fr.imageFolderList);
            fr.findViewById(R.id.llLoading).setVisibility(View.GONE);
        }
    }

    private void initAdapter(List<ImageFolderBeanT> data) {
        MyAdapter adapter = new MyAdapter(context, data);
        adapter.isShowHeaderView(true);
        // 添加标题 header
        TextView textView = new TextView(context);
        textView.setText(R.string.local_gallery_list_title);
        textView.setGravity(Gravity.CENTER);
        textView.setBackgroundColor(getResources().getColor(R.color.title_bg));
        textView.setPadding(0, (int) getResources().getDimension(R.dimen.title_padding_top)
                , 0, (int) getResources().getDimension(R.dimen.title_padding_bottom));
        adapter.addHeaderView(textView);
        adapter.setCustomFooterView(R.layout.place_holder);
        adapter.setOnItemClickListener(new OnItemClickListener<ImageFolderBeanT>() {
            @Override
            public void onItemClick(ViewHolder holder, ImageFolderBeanT data, int pos) {
                if (data.getFirstImagePath() == null) {
                    //高清图缓存目录
                    if (data.getCount() > 0) {
                        AtyLocalBigImage.start(FrLocalGallery.this, REQUEST_MY_IMAGE,
                                data.getDir(), AtyLocalBigImage.TYPE_MY_IMAGES);
                    } else {
                        //TODO: 告知用户发送的方法
                    }
                } else {
                    AtyLocalBigImage.start(FrLocalGallery.this, REQUEST_OTHER_IMAGE,
                            data.getDir(), AtyLocalBigImage.TYPE_OTHER_IMAGES);
                }
            }
        });
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        String parentPath;
        int count;
        if (requestCode == REQUEST_MY_IMAGE && resultCode == AtyLocalBigImage.RESULT_REFRESH_COUNT) {
            if (data == null) return;
            parentPath = data.getStringExtra(AtyLocalBigImage.RESULT_EXTRA_PARENT_PATH);
            count = data.getIntExtra(AtyLocalBigImage.RESULT_EXTRA_COUNT, -1);
            if (!TextUtils.isEmpty(parentPath) && count >= 0) {
                MyAdapter adapter = (MyAdapter) recyclerView.getAdapter();
                if (adapter != null)
                    adapter.setImageCount(parentPath, count);
            }
        }
    }

    /**
     * 列表适配器类
     */
    private class MyAdapter extends CommonAdapter<ImageFolderBeanT> {

        MyAdapter(Context context, @NonNull List<ImageFolderBeanT> data) {
            super(context, data);
        }

        void setImageCount(String parentPath, int count) {
            List<ImageFolderBeanT> list = getAllData();
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).getDir().equals(parentPath)) {
                    getAllData().get(i).setCount(count);
                    notifyItemDataChanged(i);
                    return;
                }
            }
        }

        @Override
        public int getItemLayoutId(int viewType) {
            return R.layout.item_lv_main;
        }

        @Override
        public void bindView(ViewHolder holder, int pos, int viewType) {
            final ImageFolderBeanT item = getItem(pos);
            if (item.getFirstImagePath() == null) {
                //高清图缓存目录
                holder.getItemView().setVisibility(View.VISIBLE);
                holder.setText(R.id.tvName, getString(R.string.ui_gallery_folder_name,
                        getString(R.string.local_gallery_list_received), item.getCount()));
                ((ImageView) holder.getView(R.id.ivIco)).setImageResource(R.mipmap.ico_app);
                return;
            }
            holder.getItemView().setVisibility(View.VISIBLE);
            holder.setText(R.id.tvName, getString(R.string.ui_gallery_folder_name,
                    item.getName(), item.getCount()));
            GlideApp.with(FrLocalGallery.this)
                    .load(item.getFirstImagePath())
                    .into((ImageView) holder.getView(R.id.ivIco));
        }
    }

    /**
     * item间距
     */
    public class SpaceItemDecoration extends RecyclerView.ItemDecoration {
        private int space;

        SpaceItemDecoration(int space) {
            this.space = space;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            if (parent.getChildPosition(view) != 0)
                outRect.top = space;
        }
    }
}
