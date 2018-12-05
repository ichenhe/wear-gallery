package cc.chenhe.weargallery;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.List;

import cc.chenhe.lib.weartools.activity.WTAppCompatActivity;
import cc.chenhe.weargallery.adapter.BaseRecyclerAdapter;
import cc.chenhe.weargallery.utils.DialogUtils;
import cc.chenhe.weargallery.utils.SendImagesManager;

/**
 * Created by 晨鹤 on 2017/2/14.
 */

public class AtyShareImage extends WTAppCompatActivity implements View.OnClickListener {

    private RecyclerView recyclerView;
    private Context context;
    private MaterialDialog sendingDialog = null;
    private SendImagesManager.OnSendStateChangedListener listener = null;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        initView();

        if (Intent.ACTION_SEND.equals(getIntent().getAction())) {
            handleSendImage(getIntent());
        } else {
            handleSendMultipleImages(getIntent());
        }

    }

    /**
     * 发送多个图片
     */
    private void handleSendMultipleImages(Intent intent) {
        ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        if (imageUris == null)
            return;
        List<String> list = new ArrayList<>();
        for (Uri uri : imageUris) {
            String path = getFilePathFromContentUri(uri, getContentResolver());
            if (path == null) {
                Toast.makeText(context, R.string.share_image_path_null, Toast.LENGTH_SHORT).show();
                continue;
            }
            list.add(path);
        }
        if (list.size() == 0) {
            finish();
            return;
        }
        recyclerView.setAdapter(new MyAdapter(this, list));
        ((TextView) findViewById(R.id.tv_photo_ok)).setText(getString(R.string.grid_pics_ok, list.size()));
    }

    /**
     * 发送单个图片
     */
    private void handleSendImage(Intent intent) {
        Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageUri != null) {
            List<String> list = new ArrayList<>();
            String path = getFilePathFromContentUri(imageUri, getContentResolver());
            if (path == null) {
                Toast.makeText(context, R.string.share_image_path_null, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            list.add(path);
            recyclerView.setAdapter(new MyAdapter(this, list));
            ((TextView) findViewById(R.id.tv_photo_ok)).setText(getString(R.string.grid_pics_ok, 1));
        }
    }

    /**
     * 单击事件
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_cancel:
                finish();
                break;
            case R.id.tv_photo_ok:
                MyAdapter adapter = (MyAdapter) recyclerView.getAdapter();
                if (adapter == null) return;
                List<String> data = adapter.getData();
                if (data == null || data.size() <= 0) return;

                SendImagesManager.getInstance().setShowResultNotification(false);
                sendingDialog = DialogUtils.getProgressDialogBuilder(context)
                        .content(getString(R.string.grid_pics_send_ing, 0, data.size()))
                        .cancelable(false)
                        .positiveText(R.string.grid_pics_send_ing_pos)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                SendImagesManager.getInstance().removeSendListener(listener);
                                SendImagesManager.getInstance().setShowResultNotification(true);
                                sendingDialog.dismiss();
                                sendingDialog = null;
                                if (!isFinishing()) finish();
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
                SendImagesManager.getInstance().addImages(data);
                SendImagesManager.getInstance().setShowResultNotification(false);
                //注册监听器
                regOnSendStateChangedListener();
                //开始发送
                SendImagesManager.getInstance().startSend(getApplicationContext());

                break;
        }
    }

    private void initView() {
        setContentView(R.layout.aty_share_image);

        Toolbar toolbar = findViewById(R.id.bar);
        toolbar.setTitle(R.string.share_image_label);
        setSupportActionBar(toolbar);

        recyclerView = findViewById(R.id.rv);
        /*这里直接设置三列表格布局*/
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new MyLinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        findViewById(R.id.tv_cancel).setOnClickListener(this);
        findViewById(R.id.tv_photo_ok).setOnClickListener(this);
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
                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                        if (!isFinishing()) finish();
                                    }
                                })
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
                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                        if (!isFinishing()) finish();
                                    }
                                })
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

    /**
     * 图片Uri转path
     *
     * @param selectedVideoUri uri
     * @param contentResolver  contentResolver
     * @return path
     */
    public String getFilePathFromContentUri(Uri selectedVideoUri,
                                            ContentResolver contentResolver) {
        String filePath;
        String[] filePathColumn = {MediaStore.MediaColumns.DATA};

        Cursor cursor = contentResolver.query(selectedVideoUri, filePathColumn, null, null, null);
        if (cursor == null)
            return null;

        cursor.moveToFirst();

        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        filePath = cursor.getString(columnIndex);
        cursor.close();
        return filePath;
    }

    private class MyAdapter extends BaseRecyclerAdapter<String, RecyclerView.ViewHolder> {

        MyAdapter(Context context, List<String> list) {
            super(context, list);
        }

        public List<String> getData() {
            return list;
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = mInflater.inflate(R.layout.rv_item_share_images, parent, false);
            return new MyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            super.onBindViewHolder(holder, position);
            MyViewHolder viewHolder = (MyViewHolder) holder;
            viewHolder.tvFileName.setText(list.get(position));
            GlideApp.with(context)
                    .load(list.get(position))
                    .placeholder(R.drawable.bg_pic_defaut)
                    .into(viewHolder.ivImage);
        }

        /**
         * 自定义ViewHolder
         */
        class MyViewHolder extends RecyclerView.ViewHolder {

            ImageView ivImage;
            TextView tvFileName;
            TextView tvFileNums;

            MyViewHolder(View itemView) {
                super(itemView);
                tvFileName = itemView.findViewById(R.id.tv_file_name);
                tvFileNums = itemView.findViewById(R.id.tv_pic_nums);
                ivImage = itemView.findViewById(R.id.iv_icon);
            }
        }
    }

    private class MyLinearLayoutManager extends LinearLayoutManager {

        MyLinearLayoutManager(Context context) {
            super(context);
        }

        public MyLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
            super(context, orientation, reverseLayout);
        }

        public MyLinearLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
            super(context, attrs, defStyleAttr, defStyleRes);
        }

        /**
         * Disable predictive animations. There is a bug in RecyclerView which causes views that
         * are being reloaded to pull invalid ViewHolders from the internal recycler stack if the
         * adapter size has decreased since the ViewHolder was recycled.
         */
        @Override
        public boolean supportsPredictiveItemAnimations() {
            return false;
        }
    }
}
