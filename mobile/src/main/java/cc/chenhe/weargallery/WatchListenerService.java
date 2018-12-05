package cc.chenhe.weargallery;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mobvoi.android.wearable.Asset;
import com.mobvoi.android.wearable.DataMap;
import com.mobvoi.android.wearable.PutDataMapRequest;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cc.chenhe.lib.weartools.WTBothway;
import cc.chenhe.lib.weartools.WTSender;
import cc.chenhe.lib.weartools.listener.WTListenerService;
import cc.chenhe.weargallery.bean.ImageFolderBean;
import cc.chenhe.weargallery.bean.eventmsg.ShowWatchRequestChangedMsg;
import cc.chenhe.weargallery.common.CUtils;
import cc.chenhe.weargallery.common.Cc;
import cc.chenhe.weargallery.common.bean.ImageFolderBeanT;
import cc.chenhe.weargallery.utils.BitmapUtils;
import cc.chenhe.weargallery.utils.HideDbHelper;
import cc.chenhe.weargallery.utils.ImageUtils;
import cc.chenhe.weargallery.utils.Settings;

/**
 * 后台监听手表请求
 * Created by 晨鹤 on 2016/12/15.
 */

public class WatchListenerService extends WTListenerService {

    private Context context;
    private Boolean toast;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        toast = Settings.showWatchRequestToast(context);
        EventBus.getDefault().register(this);
    }

    @Subscribe()
    public void onShowWatchRequestChanged(ShowWatchRequestChangedMsg msg) {
        toast = msg.show;
    }

    @Override
    public void onMessageReceived(String nodeId, String path, byte[] data, byte[] bothwayId) {
        String str = new String(data);
        if (path.equals(Cc.PATH_GET_GALLERY)) {
            do_getGallery(nodeId, path, bothwayId);
        } else if (path.startsWith(Cc.PATH_WATCH_INIT)) {
            do_watchInit(nodeId, path, bothwayId, str);
        } else if (path.startsWith(Cc.PATH_GET_MICRO_PICTURE)) {
            do_getMicroPicture(bothwayId, path, str);
        } else if (path.equals(Cc.PATH_HIDE_GALLERY)) {
            do_hideGallery(nodeId, path, bothwayId, str);
        } else if (path.equals(Cc.PATH_GET_FILE_NAME_LIST)) {
            do_getFileNameList(nodeId, path, bothwayId, str);
        } else if (path.startsWith(Cc.PATH_GET_SINGLE_PICTURE)) {
            do_getMicroPicture(bothwayId, path, str);
        } else if (path.equals(Cc.PATH_GET_ORIGINAL_PICTURE)) {
            do_getOriginalPic(bothwayId, path, str);
        } else if (path.equals(Cc.PATH_DEL_PHONE_PICTURE)) {
            do_delPhonePicture(nodeId, path, bothwayId, str);
        }
    }

    @Override
    public void onDataChanged(String path, DataMap dataMap) {
    }

    @Override
    public void onDataDeleted(String path) {

    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    /**
     * 初始化
     *
     * @param nodeId    id
     * @param bothwayId id
     * @param watchCode 手表app版本
     */
    private void do_watchInit(final String nodeId, final String path, final byte[] bothwayId, String watchCode) {
        JSONObject r = new JSONObject();
        //检查版本
        if (BuildConfig.FEATURE_VERSION != Integer.valueOf(watchCode)) {
            r.put(Cc.RESULT_INIT_KEY_OPERATION_TYPE, Cc.RESULT_INIT_OPERATION_TYPE_APP_VERSION);
            r.put(Cc.RESULT_INIT_KEY_DATA, false);
            WTBothway.response(context, nodeId, path, bothwayId, r.toString(), null);
            Toast.makeText(context, getString(R.string.watch_operation_version_different), Toast.LENGTH_SHORT).show();
            return;
        }

        //开始检查谷歌授权，跳转到谷歌监听器继续执行
//        r.put(Cc.RESULT_INIT_KEY_OPERATION_TYPE, Cc.RESULT_INIT_OPERATION_TYPE_CHECK_GOOGEL_LICENSE);
//        if (!Unit.GOOGLE_MODE) {
//            r.put(Cc.RESULT_INIT_KEY_DATA, Cc.RESULT_LICENSE_ALLOW);
//            OpenWatchBothWay.response(context, path, r.toString(), null);
        //无需检查授权，继续执行：扫描图库
        do_getGallery(nodeId, path, bothwayId);
//            return;
//        }
//        switch (GooglePlayServicesUtil.isGooglePlayServicesAvailable(context)) {
//            case ConnectionResult.SERVICE_DISABLED:
//            case ConnectionResult.SERVICE_INVALID:
//            case ConnectionResult.SERVICE_MISSING:
//                r.put(Cc.RESULT_INIT_KEY_DATA, Cc.RESULT_LICENSE_DISALLOW_OTHER);
//                OpenWatchBothWay.response(context, path, r.toString(), null);
//                return;
//        }
//        checkGoogleLicensePath = path;
//        mLisceneHandler = new Handler();
//        mLicenseCheckerCallback = new LicenseCheckerCallback();
//        mLicenseChecker = new LicenseChecker(this,
//                new ServerManagedPolicy(this, new AESObfuscator(Unit.SALT, getPackageName(), Unit.androidId))
//                , Unit.BASE64_PUBLIC_KEY);
//        mLicenseChecker.checkAccess(mLicenseCheckerCallback);
    }

    /**
     * 获取相册列表
     */
    private void do_getGallery(final String nodeId, final String path, final byte[] bothwayId) {
        if (toast)
            Toast.makeText(context, getString(R.string.watch_operation_search_gallery_ing), Toast.LENGTH_SHORT).show();
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            //没有权限
            JSONObject r = new JSONObject();
            r.put(Cc.RESULT_INIT_KEY_OPERATION_TYPE, Cc.RESULT_INIT_OPERATION_TYPE_GALLERY_LIST);
            r.put(Cc.RESULT_INIT_KEY_DATA, Cc.GET_GALLERY_ERR_PERMISSION);
            WTBothway.response(context, nodeId, path, bothwayId, r.toString(), null);
            return;
        }
        Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                //扫描媒体库完成
                if (msg.what != 0)
                    return;
                JSONObject r = new JSONObject();
                r.put(Cc.RESULT_INIT_KEY_OPERATION_TYPE, Cc.RESULT_INIT_OPERATION_TYPE_GALLERY_LIST);
                if (msg.obj == null) {
                    r.put(Cc.RESULT_INIT_KEY_DATA, Cc.GET_GALLERY_ERR_SCAN);
                    WTBothway.response(context, nodeId, path, bothwayId, r.toString(), null);
                    Toast.makeText(context, context.getString(R.string.watch_operation_search_gallery_err), Toast.LENGTH_SHORT).show();
                    return;
                }

                List<ImageFolderBean> imageFloders = (List<ImageFolderBean>) msg.obj;
                //去除已隐藏的目录
                SQLiteDatabase database = HideDbHelper.getInstance(getApplicationContext()).getReadableDatabase();
                for (int i = 0; i < imageFloders.size(); i++) {
                    Cursor cursor = database.query(HideDbHelper.TABLE_HADE_NAME, null, HideDbHelper.KEY_PARENT_PATH + "=?", new String[]{new File(imageFloders.get(i).path).getParent()}, null, null, null);
                    if (cursor.getCount() > 0) {
                        imageFloders.remove(i);
                        i--;
                    }
                    cursor.close();
                }
                database.close();

                //转换轻量级包装类
                List<ImageFolderBeanT> imageFolderTs = new ArrayList<>();
                for (int i = 0; i < imageFloders.size(); i++) {
                    ImageFolderBean bean = imageFloders.get(i);
                    imageFolderTs.add(new ImageFolderBeanT(new File(bean.path).getParent(), bean.path, bean.fileName, bean.pisNum));
                }

                //发送
                r.put(Cc.RESULT_INIT_KEY_DATA, JSON.toJSONString(imageFolderTs));
                WTBothway.response(context, nodeId, path, bothwayId, r.toString(), null);

            }
        };
        ImageUtils.loadLocalFolderContainsImage(context, mHandler, 0);
    }

    /**
     * 获取缩略图
     *
     * @param bothwayId id
     * @param pPath     原图路径
     */
    private void do_getMicroPicture(final byte[] bothwayId, final String path, final String pPath) {
        if (pPath.isEmpty())
            return;
        Log.i("请求图库缩略图", pPath);
        new OperatePictureTask(context, pPath, bothwayId, path, 450 * 1024, 1088, 1920, null).execute();
    }

    /**
     * 后台压缩图片，回传缩略图
     */
    class OperatePictureTask extends AsyncTask<Void, Void, byte[]> {
        private Context context;
        private String filePath, path;
        private byte[] bothwayId;
        private int fileSize, height, width;
        private WTSender.SendDataCallback sendListener;

        OperatePictureTask(Context context, String filePath, byte[] bothwayId, String path, int fileSize, int width, int height, WTSender.SendDataCallback sendListener) {
            this.context = context;
            this.filePath = filePath;
            this.bothwayId = bothwayId;
            this.fileSize = fileSize;
            this.path = path;
            this.height = height;
            this.width = width;
            this.sendListener = sendListener;
        }

        @Override
        protected byte[] doInBackground(Void... params) {
            Bitmap bitmap = BitmapUtils.decodeSampledBitmapFromFd(filePath, width, height);
            if (bitmap == null) {
                return null;
            }
            byte[] bytes = CUtils.bitmap2Bytes(bitmap, fileSize);
            bitmap.recycle();
            System.gc();
            return bytes;
        }

        @Override
        protected void onPostExecute(byte[] bytes) {
            super.onPostExecute(bytes);
            if (bytes == null) {
                Toast.makeText(context, "Err:Read picture null\n" + filePath, Toast.LENGTH_SHORT).show();
                return;
            }
            PutDataMapRequest putDataMapRequest = WTBothway.getPutDataMapRequest(path);
            putDataMapRequest.setUrgent();
            putDataMapRequest.getDataMap().putAsset(Cc.ASSET_KEY_PIC, Asset.createFromBytes(bytes));
            WTBothway.responseDataItem(context, bothwayId, putDataMapRequest, sendListener);
        }
    }

    /**
     * 隐藏相册
     */
    private void do_hideGallery(String nodeId, String path, byte[] bothwayId, String str) {
        SQLiteDatabase database = HideDbHelper.getInstance(context).getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(HideDbHelper.KEY_PARENT_PATH, str);
        database.insert(HideDbHelper.TABLE_HADE_NAME, null, cv);
        database.close();
        WTBothway.response(context, nodeId, path, bothwayId, "ok", null);
    }

    /**
     * 获取指定目录下的所有图片文件名
     *
     * @param parentPath 父目录
     */
    private void do_getFileNameList(final String nodeId, final String path, final byte[] bothwayId, final String parentPath) {
        if (toast)
            Toast.makeText(context, getString(R.string.watch_operation_get_pics_list_ing), Toast.LENGTH_SHORT).show();
        Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                String str = "";
                if (msg.what != 0)
                    return;
                if (msg.obj == null) {
                    //扫描图片失败
                    WTBothway.response(context, nodeId, path, bothwayId, str, null);
                    return;
                }
                ArrayList<ImageFolderBean> list = (ArrayList<ImageFolderBean>) msg.obj;
                ArrayList<String> fileNames = new ArrayList<>();
                for (ImageFolderBean item : list) {
                    fileNames.add(item.path.substring(parentPath.length() + 1));
                }
                WTBothway.response(context, nodeId, path, bothwayId, JSON.toJSONString(fileNames), null);
            }
        };
        ImageUtils.queryGalleryPicture(context, parentPath, handler, 0);
    }

    /**
     * 获取高清原图
     */
    private void do_getOriginalPic(byte[] bothwayId, String path, String filePath) {
        if (toast)
            Toast.makeText(context, getString(R.string.watch_operation_send_hd_picture_ing), Toast.LENGTH_SHORT).show();
        File file = new File(filePath);
        try (FileInputStream in = new FileInputStream(file)) {
            int len = in.available();
            byte buffer[] = new byte[len];
            in.read(buffer);

            PutDataMapRequest putDataMapRequest = WTBothway.getPutDataMapRequest(path);
            putDataMapRequest.setUrgent();
            putDataMapRequest.getDataMap().putString(Cc.RESULT_IMAGE_EXTENSION_NAME, CUtils.getExtraName(file));
            putDataMapRequest.getDataMap().putAsset(Cc.ASSET_KEY_PIC, Asset.createFromBytes(buffer));
            WTBothway.responseDataItem(context, bothwayId, putDataMapRequest, null);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, getString(R.string.watch_operation_send_hd_picture_fail), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 删除手机图片
     */
    private void do_delPhonePicture(String nodeId, String path, byte[] bothwayId, String str) {
        if (toast)
            Toast.makeText(context, "删除图片：" + str, Toast.LENGTH_SHORT).show();
        int r;
        File file = new File(str);
        if (!file.exists()) {
            WTBothway.response(context, nodeId, path, bothwayId, String.valueOf(Cc.RESULT_DEL_PHONE_PICTURE_OK), null);
            return;
        }
        String params[] = new String[]{str};
        int d = 0;
        try {
            d = getContentResolver().delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaStore.Images.Media.DATA + " = ?", params);
        } catch (SecurityException ignore) {
        }
        if (d > 0) {
            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri uri = Uri.fromFile(file);
            intent.setData(uri);
            context.sendBroadcast(intent);
            if (!file.exists()) {
                r = Cc.RESULT_DEL_PHONE_PICTURE_OK;
            } else {
                r = Cc.RESULT_DEL_PHONE_PICTURE_NO_PERMISSION;
            }
        } else {
            r = Cc.RESULT_DEL_PHONE_PICTURE_FAILED;
        }
        WTBothway.response(context, nodeId, path, bothwayId, String.valueOf(r), null);
    }
}
