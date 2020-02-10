package cc.chenhe.weargallery;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.mobvoi.android.wearable.Asset;
import com.mobvoi.android.wearable.DataMap;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.InputStream;
import java.lang.ref.WeakReference;

import cc.chenhe.lib.weartools.AssetHelper;
import cc.chenhe.lib.weartools.WTBothway;
import cc.chenhe.lib.weartools.WTSender;
import cc.chenhe.lib.weartools.listener.WTListenerService;
import cc.chenhe.weargallery.activity.AtyReceivingPics;
import cc.chenhe.weargallery.bean.eventmsg.LocalImageChangedMsg;
import cc.chenhe.weargallery.common.CUtils;
import cc.chenhe.weargallery.common.Cc;
import cc.chenhe.weargallery.uilts.Utils;

import static cc.chenhe.weargallery.common.CUtils.getFolderSize;

/**
 * Created by 晨鹤 on 2016/12/17.
 */

public class MobileListenerService extends WTListenerService {
    public static final String ACTION_RECEIVE_IMAGES_SUCCESS = "ACTION_RECEIVE_IMAGES_SUCCESS";
    public static final String ACTION_RECEIVE_IMAGES_FAILED = "ACTION_RECEIVE_IMAGES_FAILED";
    public static final int FAILED_SEND_RECEIPT_MESSAGE = -1;
    public static final int FAILED_WAIT_NEXT_DATA_TIME_OUT = -2;
    public static final String ACTION_RECEIVE_IMAGES_ITEM = "ACTION_RECEIVE_IMAGES_ITEM";
    public static final String ACTION_RECEIVE_IMAGES_CANCEL = "ACTION_RECEIVE_IMAGES_CANCEL";

    private static final int WHAT_SEND_IMAGE_TIME_OUT = 100;

    private Context context;
    /*用于检测批量发送图片超时*/
    private ReceiveImagesHandler handler;
//    private Timer timer;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
    }

    @Override
    public void onMessageReceived(String nodeId, String path, byte[] data, byte[] bothwayId) {
        switch (path) {
            case Cc.PATH_DEL_CACHE:
            case Cc.PATH_DEL_OFFLINE_IMAGE:
                do_delCache(nodeId, bothwayId, path);
                break;
            case Cc.PATH_GET_WATCH_CACHE_SIZE:
                do_getCacheSize(nodeId, path, bothwayId);
                break;
            case Cc.PATH_SEND_PICS_FINISH:
                do_sendImagesFinish();
                break;
            case Cc.PATH_SEND_PICS_CANCEL:
                do_sendImagesCancel();
                break;
        }
    }

    @Override
    public void onDataChanged(String path, DataMap dataMap) {
        if (path.equals(Cc.PATH_SEND_PICS))
            do_receiveImages(dataMap);
    }

    @Override
    public void onDataDeleted(String path) {

    }

    /**
     * 手机确认批量图片发送完毕
     */
    private void do_sendImagesFinish() {
        if (handler != null) {
            handler.removeMessages(WHAT_SEND_IMAGE_TIME_OUT);
            handler = null;
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ACTION_RECEIVE_IMAGES_SUCCESS));
    }

    private void do_sendImagesCancel() {
        if (handler != null) {
            handler.removeMessages(WHAT_SEND_IMAGE_TIME_OUT);
            handler = null;
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ACTION_RECEIVE_IMAGES_CANCEL));
    }

    /**
     * 接收批量发送的图片
     */
    private void do_receiveImages(final DataMap dataMap) {
        if (System.currentTimeMillis() - dataMap.getLong(Cc.KEY_SEND_TIME, 0) > CUtils.SEND_IMAGES_DATA_TIME_OUT) {
            //超时后的数据不再处理
            return;
        }
        if (handler != null)
            handler.removeMessages(WHAT_SEND_IMAGE_TIME_OUT);
        //启动提示界面
        Intent intent = new Intent(context, AtyReceivingPics.class);
        intent.putExtra("sendedCount", dataMap.getInt(Cc.KEY_SENDED_COUNT));
        intent.putExtra("totalCount", dataMap.getInt(Cc.KEY_TOTAL_COUNT));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);

        //发送更新广播
        Intent intent1 = new Intent(ACTION_RECEIVE_IMAGES_ITEM);
        intent1.putExtra("sendedCount", dataMap.getInt(Cc.KEY_SENDED_COUNT));
        intent1.putExtra("totalCount", dataMap.getInt(Cc.KEY_TOTAL_COUNT));
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent1);
        //取得数据
        Asset asset = dataMap.getAsset(Cc.ASSET_KEY_PIC);
        AssetHelper.get(context, asset, new AssetHelper.AssetCallback() {
            @Override
            public void onResult(InputStream ins) {
                String filePathOnMobile = dataMap.getString(Cc.KEY_FILE_PATH_ON_PHONE);
                CUtils.writeImageToDisk(ins, Utils.Path_cache_original + filePathOnMobile.hashCode() + ".webp");
                //回执给手机发送成功
                WTSender.sendMessage(context, Cc.PATH_CONFIRM_RECEIVE_PICS, "", new WTSender.SendMsgCallback() {
                    @Override
                    public void onSuccess() {
                        if (handler == null)
                            handler = new ReceiveImagesHandler(MobileListenerService.this);
                        handler.sendEmptyMessageDelayed(WHAT_SEND_IMAGE_TIME_OUT,
                                CUtils.SEND_IMAGES_DATA_TIME_OUT / 2);
                    }

                    @Override
                    public void onFailed(int resultCode) {
                        Intent i = new Intent(ACTION_RECEIVE_IMAGES_FAILED);
                        i.putExtra("resultCode", FAILED_SEND_RECEIPT_MESSAGE);
                        LocalBroadcastManager.getInstance(context).sendBroadcast(i);
                    }
                });
            }
        });
    }

    private void do_delCache(String nodeId, byte[] bothwayId, String path) {
        switch (path) {
            case Cc.PATH_DEL_CACHE:
                CUtils.deleteFile(new File(Utils.Path_cache_gallery));
                CUtils.deleteFile(new File(Utils.Path_cache_single));
                break;
            case Cc.PATH_DEL_OFFLINE_IMAGE:
                CUtils.deleteFile(new File(Utils.Path_cache_original));
                EventBus.getDefault().post(new LocalImageChangedMsg());
                break;
        }
        Utils.initFolder(this);
        WTBothway.response(context, nodeId, path, bothwayId, "ok", null);
    }

    private void do_getCacheSize(String nodeId, String path, byte[] bothwayId) {
        long cacheSize = 0;
        long originalSize = 0;
        try {
            cacheSize = getFolderSize(new File(Utils.Path_cache_gallery));
            cacheSize += getFolderSize(new File(Utils.Path_cache_single));
            originalSize = getFolderSize(new File(Utils.Path_cache_original));
        } catch (Exception e) {
            e.printStackTrace();
            WTBothway.response(context, nodeId, path, bothwayId, "err", null);
        }
        WTBothway.response(context, nodeId, path, bothwayId,
                CUtils.getFormatSize(cacheSize) + "|"
                        + CUtils.getFormatSize(originalSize), null);
    }

    private class ReceiveImagesHandler extends Handler {
        WeakReference<MobileListenerService> wr;

        ReceiveImagesHandler(@NonNull MobileListenerService service) {
            wr = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            MobileListenerService service = wr.get();
            if (service == null) return;
            if (msg.what == WHAT_SEND_IMAGE_TIME_OUT) {
                Intent i = new Intent(ACTION_RECEIVE_IMAGES_FAILED);
                i.putExtra("resultCode", FAILED_WAIT_NEXT_DATA_TIME_OUT);
                LocalBroadcastManager.getInstance(context).sendBroadcast(i);
            }
        }
    }
}
