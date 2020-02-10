package cc.chenhe.weargallery;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Observable;
import java.util.Observer;

import cc.chenhe.weargallery.adapter.ImageGridApter;
import cc.chenhe.weargallery.bean.ImageFolderBean;
import cc.chenhe.weargallery.listener.OnRecyclerViewClickListener;
import cc.chenhe.weargallery.utils.DialogUtils;
import cc.chenhe.weargallery.utils.ImageSelectObservable;
import cc.chenhe.weargallery.utils.ImageUtils;
import cc.chenhe.weargallery.utils.SendImagesManager;

/**
 * Created by 晨鹤 on 2016/12/2.
 */

public class AtyGridPics extends AtyBase implements Handler.Callback, Observer, OnRecyclerViewClickListener, View.OnClickListener {
    private static final int MSG_LOAD_PICS = 11;
    private static final int REQUEST_PREVIEW_PICS = 10;

    private MaterialDialog sendingDialog = null;
    private Context context;
    private SendImagesManager.OnSendStateChangedListener listener = null;
    private MenuItem sendMenu;

    public static void startGridPicsActivity(Activity activity, String folder, boolean singleSelect, int maxCount) {
        Intent intent = new Intent(activity, AtyGridPics.class);
        intent.putExtra("data", folder);
        intent.putExtra("single", singleSelect);
        intent.putExtra("maxCount", maxCount);
        activity.startActivity(intent);
    }

    /**
     * 图片选择适配器
     */
    private ImageGridApter mAdapter;
    private RecyclerView recyclerView;
    private TextView mOkTv;
    private Toolbar toolbar;
    private View footerOperation;
    private Handler mHandler;

    @Override
    public void onBackPressed() {
        if (mAdapter.isSelectMode()) {
            setSelectMode(false, -1);
        } else {
            super.onBackPressed();
        }

    }

    @Override
    protected void onDestroy() {
        ImageSelectObservable.getInstance().deleteObserver(this);
        ImageSelectObservable.getInstance().clearAndRelease();
        SendImagesManager.getInstance().removeSendListener(listener);
        super.onDestroy();
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        initView();
        ImageSelectObservable.getInstance().addObserver(this);
        mHandler = new Handler(this);
        initData();

        //滑动时停止加载
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    if (!isFinishing())
                        GlideApp.with(context).pauseRequests();
                } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if (!isFinishing())
                        GlideApp.with(context).resumeRequests();
                }
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.grid_pics, menu);
        sendMenu = menu.findItem(R.id.menu_send);
        return true;
    }

    private void initView() {
        setContentView(R.layout.aty_grid_photo);

        toolbar = findViewById(R.id.bar);
        String s[] = getIntent().getStringExtra("data").split("/");
        toolbar.setTitle(s[s.length - 1]);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.menu_send) {
                    setSelectMode(true, -1);
                }
                return false;
            }
        });

        footerOperation = findViewById(R.id.ll_photo_operation);
        footerOperation.setVisibility(View.GONE);
        recyclerView = findViewById(R.id.rv);
        /*这里直接设置三列表格布局*/
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setHasFixedSize(true);

        mAdapter = new ImageGridApter(this, ImageSelectObservable.getInstance().getFolderAllImages(), false, getIntent().getIntExtra("maxCount", 9), false);
        mAdapter.setOnClickListener(this);
        recyclerView.setAdapter(mAdapter);

        //确认发送按钮
        mOkTv = findViewById(R.id.tv_photo_ok);
        mOkTv.setText(String.format(getResources().getString(R.string.grid_pics_ok), mAdapter.getSelectlist().size()));
        mOkTv.setOnClickListener(this);
        //预览按钮
        findViewById(R.id.tv_photo_scan).setOnClickListener(this);
    }

    /**
     * 始化数据
     */
    private void initData() {
        ImageUtils.queryGalleryPicture(this, getIntent().getStringExtra("data"), mHandler, MSG_LOAD_PICS);
    }

    private void setSelectMode(boolean isSelectMode, int position) {
        if (isSelectMode && mAdapter != null && !mAdapter.isSelectMode()) {
            footerOperation.setVisibility(View.VISIBLE);
            mAdapter.setSelectMode(true, position);
            sendMenu.setVisible(false);
        } else if (!isSelectMode && mAdapter != null && mAdapter.isSelectMode()) {
            footerOperation.setVisibility(View.GONE);
            ImageSelectObservable.getInstance().clearSelectImgs();
            ImageSelectObservable.getInstance().updateImageSelectChanged();
            mAdapter.setSelectMode(false);
            sendMenu.setVisible(true);
        }
    }

    /**
     * 注册发送监听器
     */
    private void regOnSendStateChangedListener() {
        if (listener == null)
            listener = new SendImagesManager.OnSendStateChangedListener() {
                @Override
                public void onSendSuccess(int totalCount) {
                    if (sendingDialog != null) {
                        sendingDialog.dismiss();
                        sendingDialog = null;

                        DialogUtils.getBasicDialogBuilder(context)
                                .title(R.string.grid_pics_send_success_title)
                                .content(getString(R.string.grid_pics_send_success_content, totalCount))
                                .positiveText(R.string.confirm)
                                .build().show();
                    }

                    //移除发送监听器
                    SendImagesManager.getInstance().removeSendListener(listener);
                }

                @Override
                public void onSendFailed(int successCount, int totalCount, String sendingImage,
                                         int resultCode) {
                    if (sendingDialog != null) {
                        sendingDialog.dismiss();
                        sendingDialog = null;

                        DialogUtils.getBasicDialogBuilder(context)
                                .title(R.string.grid_pics_send_failed_title)
                                .content(getString(R.string.grid_pics_send_failed_content, SendImagesManager.getResultDescribe(context, resultCode), successCount))
                                .positiveText(R.string.confirm)
                                .build().show();
                    }

                    //移除发送监听器
                    SendImagesManager.getInstance().removeSendListener(listener);
                }

                @Override
                public void onItemSendStateChanged(int stateCode) {
                    if (sendingDialog != null)
                        sendingDialog.setContent(getString(R.string.grid_pics_send_ing
                                , SendImagesManager.getInstance().getSentCount()
                                , SendImagesManager.getInstance().getTotalCount()));
                }

                @Override
                public void onQueueChanged() {
                    if (sendingDialog != null)
                        sendingDialog.setContent(getString(R.string.grid_pics_send_ing
                                , SendImagesManager.getInstance().getSentCount()
                                , SendImagesManager.getInstance().getTotalCount()));
                }

                @Override
                public void onCancel(int successCount, int totalCount) {
                    if (sendingDialog != null)
                        sendingDialog.dismiss();
                    SendImagesManager.getInstance().removeSendListener(listener);
                }
            };
        SendImagesManager.getInstance().addSendListener(listener);
    }


    @Override
    public boolean handleMessage(Message msg) {
        if (msg.what == MSG_LOAD_PICS) {
            //图片扫描完成
            ImageSelectObservable.getInstance().addFolderImagesAndClearBefore((Collection<? extends ImageFolderBean>) msg.obj);
            mAdapter.notifyDataSetChanged();
        }
        return false;
    }

    /*选中的图片改变*/
    @Override
    public void update(Observable observable, Object o) {
        mAdapter.notifyDataSetChanged();
        mOkTv.setText(getString(R.string.grid_pics_ok, mAdapter.getSelectlist().size()));
    }

    /*单击进入大图*/
    @Override
    public void onItemClick(View view, int position) {
        if (position >= 0) {
            AtyPreviewPics.startPreviewPhotoActivityForResult(this, position, REQUEST_PREVIEW_PICS, mAdapter.isSelectMode());
        }
        mOkTv.setText(getResources().getString(R.string.grid_pics_ok, mAdapter.getSelectlist().size()));
    }

    /*长按进入多选模式*/
    @Override
    public void onItemLongClick(View view, int position) {
        setSelectMode(true, position);
    }

    /**
     * 按钮被点击
     */
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_photo_ok:
                List<ImageFolderBean> images = ImageSelectObservable.getInstance().getSelectImages();
                if (images.size() == 0) {
                    //未选中图片
                    Toast.makeText(context, R.string.grid_pics_select_none_send, Toast.LENGTH_SHORT).show();
                    return;
                }
                List<String> names = new ArrayList<>();
                for (ImageFolderBean image : images) {
                    names.add(image.path);
                }
                SendImagesManager.getInstance().setShowResultNotification(false);
                sendingDialog = DialogUtils.getProgressDialogBuilder(AtyGridPics.this)
                        .content(getString(R.string.grid_pics_send_ing, 0, names.size()))
                        .cancelable(false)
                        .positiveText(R.string.grid_pics_send_ing_pos)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                SendImagesManager.getInstance().removeSendListener(listener);
                                SendImagesManager.getInstance().setShowResultNotification(true);
                                sendingDialog.dismiss();
                                sendingDialog = null;
                            }
                        })
                        .negativeText(R.string.cancel)
                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                SendImagesManager.getInstance().cancel();
                            }
                        })
                        .build();
                sendingDialog.show();
                //添加队列
                SendImagesManager.getInstance().addImages(names);
                SendImagesManager.getInstance().setShowResultNotification(false);
                //注册监听器
                regOnSendStateChangedListener();
                //开始发送
                SendImagesManager.getInstance().startSend(getApplicationContext());
                //退出选择模式
                footerOperation.setVisibility(View.GONE);
                ImageSelectObservable.getInstance().clearSelectImgs();
                ImageSelectObservable.getInstance().updateImageSelectChanged();
                mAdapter.setSelectMode(false);
                break;
            case R.id.tv_photo_scan:
                //预览已选择图片
                if (ImageSelectObservable.getInstance().getSelectImages().size() <= 0) {
                    Toast.makeText(context, getString(R.string.grid_pics_select_none_preview), Toast.LENGTH_SHORT).show();
                    return;
                }
                AtyPreviewPics.startPreviewActivity(AtyGridPics.this, REQUEST_PREVIEW_PICS);
                break;
        }
    }
}
