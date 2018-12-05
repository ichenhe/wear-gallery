package cc.chenhe.weargallery;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Objects;

import cc.chenhe.weargallery.adapter.HideFoldersAdapter;
import cc.chenhe.weargallery.listener.OnRecyclerViewClickListener;
import cc.chenhe.weargallery.utils.HideDbHelper;

/**
 * Created by 晨鹤 on 2016/12/13.
 */

public class AtyHideFolders extends AtyBase {

    private Context context;
    private Toolbar bar;
    private RecyclerView recyclerView;
    private View tvTip;
    private boolean hasChanged = false;
    private HideFoldersAdapter adapter;


    @Override
    public void onBackPressed() {
        if (hasChanged) {
            setResult(1);
        }
        super.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.aty_hide_folders);
        setResult(0);
        context = this;
        initView();
        initData();
        initEvent();
    }

    /**
     * 初始化取消隐藏按钮事件
     */
    private void initEvent() {
        if (adapter == null) {
            return;
        }
        adapter.setOnClickListener(new OnRecyclerViewClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                //item取消隐藏按钮被点击
                hasChanged = true;
                SQLiteDatabase database = HideDbHelper.getInstance(getApplicationContext()).getWritableDatabase();
                database.delete(HideDbHelper.TABLE_HADE_NAME, HideDbHelper.KEY_PARENT_PATH + "=?", new String[]{adapter.getData().get(position)});
                database.close();
                adapter.remove(position);
                if (adapter.getItemCount() == 0) {
                    recyclerView.setVisibility(View.GONE);
                    tvTip.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onItemLongClick(View view, int position) {
                //这个不会被调用
            }
        });
    }

    /**
     * 加载已隐藏的文件夹列表
     */
    private void initData() {
        SQLiteDatabase database = HideDbHelper.getInstance(getApplicationContext()).getWritableDatabase();
        Cursor cursor = database.query(HideDbHelper.TABLE_HADE_NAME, null, null, null, null, null, null);
        if (cursor == null) {
            Toast.makeText(context, getString(R.string.err_database), Toast.LENGTH_SHORT).show();
            database.close();
            finish();
            return;
        }
        if (cursor.getCount() == 0) {
            //若没有item则显示提示
            recyclerView.setVisibility(View.GONE);
            tvTip.setVisibility(View.VISIBLE);
            cursor.close();
            database.close();
            return;
        }
        cursor.moveToPrevious();
        ArrayList<String> datas = new ArrayList<>();
        while (cursor.moveToNext()) {
            String tem = cursor.getString(cursor.getColumnIndex(HideDbHelper.KEY_PARENT_PATH));
            datas.add(tem);
        }
        if (cursor.getCount() > 0) {
            tvTip.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter = new HideFoldersAdapter(context, datas);
            recyclerView.setAdapter(adapter);
        } else {
            tvTip.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        }
        cursor.close();
        database.close();
    }

    private void initView() {
        bar = findViewById(R.id.bar);
        bar.setTitle(getString(R.string.hide_title));
        setSupportActionBar(bar);
        tvTip = findViewById(R.id.tvTip);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        recyclerView = findViewById(R.id.listView);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

}
