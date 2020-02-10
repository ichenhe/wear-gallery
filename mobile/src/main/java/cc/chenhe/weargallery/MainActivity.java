package cc.chenhe.weargallery;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import cc.chenhe.lib.weartools.activity.WTAppCompatActivity;
import cc.chenhe.weargallery.adapter.ImageFolderAdapter;
import cc.chenhe.weargallery.bean.ImageFolderBean;
import cc.chenhe.weargallery.common.CUtils;
import cc.chenhe.weargallery.listener.OnRecyclerViewClickListener;
import cc.chenhe.weargallery.utils.DialogUtils;
import cc.chenhe.weargallery.utils.HideDbHelper;
import cc.chenhe.weargallery.utils.ImageFolderSelectObservable;
import cc.chenhe.weargallery.utils.ImageUtils;
import cc.chenhe.weargallery.utils.Settings;

public class MainActivity extends WTAppCompatActivity implements Handler.Callback, OnRecyclerViewClickListener, Observer, View.OnClickListener {

    private static final int MSG_LOAD_PICS_FOLDERS = 10;

    private static final int REQUEST_SETTING = 0;
    private static final int REQUEST_INTRO = 1;

    private Context context;
    private RecyclerView rv;
    private View llHideBar;
    private TextView btnSelectAll, btnHide;
    private MenuItem hideMenu;

    /**
     * 图片所在文件夹适配器
     */
    private ImageFolderAdapter folderAdapter;
    /**
     * 图片列表
     */
    ArrayList<ImageFolderBean> imageFolderList;
    private Handler handler;

    @Override
    public void onBackPressed() {
        if (folderAdapter.isSelectMode()) {
            setSelectMode(false, 0);
        } else {
            super.onBackPressed();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        imageFolderList = new ArrayList<>();
        handler = new Handler(this);
        initView();

        //检查权限
        if (!checkPermission() || Settings.lastStartVersion(context) < CUtils.getVersionCode(context)) {
            startActivityForResult(new Intent(context, AtyIntroduce.class), REQUEST_INTRO);
        } else {
            ImageUtils.loadLocalFolderContainsImage(context, handler, MSG_LOAD_PICS_FOLDERS);
        }

        folderAdapter = new ImageFolderAdapter(this, imageFolderList);
        rv.setAdapter(folderAdapter);
        folderAdapter.setOnClickListener(this);
    }

    private boolean checkPermission() {
        return !(Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest
                .permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        hideMenu = menu.findItem(R.id.menu_hide);
        return true;
    }

    private void initView() {
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.bar);
        toolbar.setTitle(getString(R.string.app_name));
        setSupportActionBar(toolbar);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_hide:
                        setSelectMode(true, -1);
                        break;
                    case R.id.menu_setting:
                        startActivityForResult(new Intent(context, AtySetting.class), REQUEST_SETTING);
                        break;
                }
                return false;
            }
        });

        rv = findViewById(R.id.rvPicsFolder);
        rv.setItemAnimator(new DefaultItemAnimator());
        rv.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new MyLinearLayoutManager(this);
        rv.setLayoutManager(layoutManager);

        rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING)
                    folderAdapter.setScrolling(true);
                else if (newState == RecyclerView.SCROLL_STATE_IDLE)
                    folderAdapter.setScrolling(false);
            }
        });

        llHideBar = findViewById(R.id.llHideBar);
        llHideBar.setVisibility(View.GONE);
        btnSelectAll = findViewById(R.id.tvSelectAll);
        btnSelectAll.setTag(false);
        btnSelectAll.setText(getString(R.string.main_hide_bar_select_all));
        btnHide = findViewById(R.id.tvHide);
        btnSelectAll.setOnClickListener(this);
        btnHide.setOnClickListener(this);

    }

    @Override
    public boolean handleMessage(Message msg) {
        if (msg.what == MSG_LOAD_PICS_FOLDERS) {
            //图片目录扫描完成
            imageFolderList.clear();
            imageFolderList.addAll((Collection<? extends ImageFolderBean>) msg.obj);
            //去除已隐藏的文件夹
            SQLiteDatabase database = HideDbHelper.getInstance(getApplicationContext()).getReadableDatabase();
            for (int i = 0; i < imageFolderList.size(); i++) {
                Cursor cursor = database.query(HideDbHelper.TABLE_HADE_NAME, null, HideDbHelper.KEY_PARENT_PATH + "=?", new String[]{new File(imageFolderList.get(i).path).getParent()}, null, null, null);
                if (cursor.getCount() > 0) {
                    imageFolderList.remove(i);
                    i--;
                }
                cursor.close();
            }
            database.close();
            folderAdapter.notifyDataSetChanged();
        }
        return false;
    }

    /**
     * 选中所有文件夹
     */
    private void selectAll(boolean isSelect) {
        if (!folderAdapter.isSelectMode())
            return;
        if (isSelect)
            ImageFolderSelectObservable.getInstance().addFoldersAndClearBefore(imageFolderList);
        else
            ImageFolderSelectObservable.getInstance().getSelectFolders().clear();
        ImageFolderSelectObservable.getInstance().setChangedPosition(-1);
        ImageFolderSelectObservable.getInstance().updateFolderSelectChanged();
    }

    /**
     * 进入/退出选择模式
     * 对各种相关变量进行设置
     *
     * @param isSelectMode 模式
     * @param position     长按的item，仅isSelectMode=true时有效
     */
    private void setSelectMode(boolean isSelectMode, int position) {
        if (isSelectMode && !folderAdapter.isSelectMode()) {
            hideMenu.setVisible(false);
            llHideBar.setVisibility(View.VISIBLE);
            folderAdapter.setSelectMode(true);
            ImageFolderSelectObservable.getInstance().addObserver(this);
            if (position >= 0)
                ImageFolderSelectObservable.getInstance().getSelectFolders().add(imageFolderList.get(position));
            ImageFolderSelectObservable.getInstance().updateFolderSelectChanged();
        } else if (!isSelectMode && folderAdapter.isSelectMode()) {
            hideMenu.setVisible(true);
            folderAdapter.setSelectMode(false);
            folderAdapter.notifyDataSetChanged();
            ImageFolderSelectObservable.getInstance().deleteObserver(this);
            ImageFolderSelectObservable.getInstance().clearAndRelease();
            llHideBar.setVisibility(View.GONE);
            btnSelectAll.setTag(false);
            btnSelectAll.setText(getString(R.string.main_hide_bar_select_all));
        }
    }

    /**
     * RecyclerViewItem单击
     **/
    @Override
    public void onItemClick(View view, int position) {
        if (folderAdapter.isSelectMode()) {
            if (ImageFolderSelectObservable.getInstance().getSelectFolders().contains(imageFolderList.get(position))) {
                //已经选中，则取消
                ImageFolderSelectObservable.getInstance().getSelectFolders().remove(imageFolderList.get(position));
            } else {
                ImageFolderSelectObservable.getInstance().getSelectFolders().add(imageFolderList.get(position));
            }
            ImageFolderSelectObservable.getInstance().setChangedPosition(position);
            ImageFolderSelectObservable.getInstance().updateFolderSelectChanged();
        } else {
            File file = new File(imageFolderList.get(position).path);
            AtyGridPics.startGridPicsActivity(this, file.getParentFile().getAbsolutePath(), false, 20);
        }
    }

    /**
     * RecyclerViewItem长按
     **/
    @Override
    public void onItemLongClick(View view, int position) {
        setSelectMode(true, position);
    }

    /**
     * 文件夹选中状态更新
     */
    @Override
    public void update(Observable observable, Object o) {
        btnHide.setText(getString(R.string.main_hide_bar_hide, ImageFolderSelectObservable.getInstance().getSelectFolders().size()));
        if (ImageFolderSelectObservable.getInstance().getChangedPosition() == -1)
            folderAdapter.notifyDataSetChanged();
        else
            folderAdapter.notifyItemChanged(ImageFolderSelectObservable.getInstance().getChangedPosition());
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tvSelectAll:
                //全选/反选
                selectAll(!(Boolean) btnSelectAll.getTag());
                btnSelectAll.setTag(!(Boolean) btnSelectAll.getTag());
                if ((Boolean) btnSelectAll.getTag())
                    btnSelectAll.setText(getString(R.string.main_hide_bar_select_none));
                else
                    btnSelectAll.setText(getString(R.string.main_hide_bar_select_all));
                break;
            case R.id.tvHide:
                //确认隐藏
                hideGallery();
                break;
        }
    }

    /**
     * 隐藏选中的相册
     */
    private void hideGallery() {
        if (folderAdapter == null || !folderAdapter.isSelectMode() || ImageFolderSelectObservable.getInstance().getSelectFolders() == null)
            return;
        DialogUtils.getBasicDialogBuilder(context)
                .title(R.string.main_hide_gallery_title)
                .content(getString(R.string.main_hide_gallery_content, ImageFolderSelectObservable.getInstance().getSelectFolders().size()))
                .positiveText(R.string.confirm)
                .negativeText(R.string.cancel)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        SQLiteDatabase database = HideDbHelper.getInstance(getApplicationContext()).getWritableDatabase();
                        List<ImageFolderBean> selectedFolders = ImageFolderSelectObservable.getInstance().getSelectFolders();
                        for (int i = 0; i < selectedFolders.size(); i++) {
                            ContentValues cv = new ContentValues();
                            //这里path取父目录
                            File file = new File(selectedFolders.get(i).path);
                            cv.put(HideDbHelper.KEY_PARENT_PATH, file.getParent());
                            database.insert(HideDbHelper.TABLE_HADE_NAME, null, cv);
                        }
                        database.close();
                        setSelectMode(false, 0);
                        ImageUtils.loadLocalFolderContainsImage(context, handler, MSG_LOAD_PICS_FOLDERS);
                    }
                }).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_INTRO:
                if (Build.VERSION.SDK_INT < 23 || checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) {
                    ImageUtils.loadLocalFolderContainsImage(context, handler, MSG_LOAD_PICS_FOLDERS);
                    Settings.lastStartVersion(context, CUtils.getVersionCode(context));
                } else {
                    if (!isFinishing())
                        finish();
                }
                break;
            case REQUEST_SETTING:
                if (resultCode == AtySetting.RESULT_NEED_REFRESH) {
                    //需要刷新图片列表
                    if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_DENIED) {
                        Toast.makeText(context, R.string.permission_err, Toast.LENGTH_SHORT).show();
                    } else {
                        ImageUtils.loadLocalFolderContainsImage(context, handler, MSG_LOAD_PICS_FOLDERS);
                    }
                }
                break;
        }
    }

    private class MyLinearLayoutManager extends LinearLayoutManager {

        MyLinearLayoutManager(Context context) {
            super(context);
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
