package cc.chenhe.weargallery;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.mobvoi.android.wearable.Asset;
import com.mobvoi.android.wearable.PutDataMapRequest;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;

import cc.chenhe.lib.weartools.WTBothway;
import cc.chenhe.lib.weartools.WTRegister;
import cc.chenhe.lib.weartools.WTSender;
import cc.chenhe.lib.weartools.listener.WTMessageListener;
import cc.chenhe.weargallery.common.CUtils;
import cc.chenhe.weargallery.common.Cc;
import cc.chenhe.weargallery.utils.SendImagesManager;
import cc.chenhe.weargallery.utils.Utils;

/**
 * 批量发送图片服务
 * Created by 晨鹤 on 2016/12/22.
 */

public class SendPicsService extends Service {
    /**
     * 通知栏取消发送按钮
     */
    public static final String ACTION_CANCEL = "wg.cancel_send_image";

    private static final int NOTIFY_ON_GOING_ID = 2;
    private static final int WHAT_TIME_OUT = 100;

    private MyHandler handler;
    private boolean flag;//true则继续运行
    private boolean isSending = false;
    private Context context;

    @Override
    public void onDestroy() {
        if (isSending)
            WTSender.sendMessage(context, Cc.PATH_SEND_PICS_CANCEL, "", null);
        stopForeground(true);
        if (messageListener != null)
            WTRegister.removeMessageListener(this, messageListener);
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        handler = new MyHandler(this);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isSending) {
            startForeground(NOTIFY_ON_GOING_ID, getSendingNotify(getString(R.string.notify_send_images_title)
                    , getString(R.string.notify_send_images_ticker)
                    , getString(R.string.notify_send_images_content,
                            SendImagesManager.getInstance().getSentCount(),
                            SendImagesManager.getInstance().getTotalCount())));
            send();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private static class MyHandler extends Handler {
        WeakReference<SendPicsService> wr;

        MyHandler(SendPicsService service) {
            wr = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            SendPicsService service = wr.get();
            if (service == null) return;
            if (msg.what == WHAT_TIME_OUT) {
                service.sendFailed(SendImagesManager.RESULT_SEND_DATA_TIME_OUT);
            }
        }
    }

    WTMessageListener messageListener = new WTMessageListener() {
        @Override
        public void onMessageReceived(String nodeId, String path, byte[] data, byte[] bothwayId) {
            if (path.equals(Cc.PATH_CONFIRM_RECEIVE_PICS) && flag) {
                handler.removeMessages(WHAT_TIME_OUT);
                WTRegister.removeMessageListener(context, messageListener);
                SendImagesManager.getInstance().onItemSendStateChanged(SendImagesManager.STATE_SEND_ITEM_SUCCESS);
                send();
            }
        }
    };

    WTSender.SendDataCallback sendDataCallback = new WTSender.SendDataCallback() {
        @Override
        public void onSuccess(Uri uri) {
            //仅仅发送成功，暂未确认接收
            //注册msg监听器
            WTRegister.addMessageListener(context, messageListener);
            handler.sendEmptyMessageDelayed(WHAT_TIME_OUT, CUtils.SEND_IMAGES_DATA_TIME_OUT);
        }

        @Override
        public void onFailed(int resultCode) {
            sendFailed(SendImagesManager.RESULT_SEND_DATA_FAIL);
        }
    };

    private void send() {
        updateOngoingNotify(getString(R.string.notify_send_images_title)
                , getString(R.string.notify_send_images_content, SendImagesManager.getInstance().getSentCount(), SendImagesManager.getInstance().getTotalCount())
                , getString(R.string.notify_send_images_content, SendImagesManager.getInstance().getSentCount(), SendImagesManager.getInstance().getTotalCount()));
        String imagePath = SendImagesManager.getInstance().getNextImagePathToSend();
        if (imagePath == null) {
            sendFinish();
        } else {
            flag = true;
            isSending = true;

            try (FileInputStream in = new FileInputStream(imagePath)) {
                int len = in.available();
                byte buffer[] = new byte[len];
                in.read(buffer);
                PutDataMapRequest putDataMapRequest = WTBothway.getPutDataMapRequest(Cc.PATH_SEND_PICS);
                putDataMapRequest.setUrgent();
                putDataMapRequest.getDataMap().putAsset(Cc.ASSET_KEY_PIC, Asset.createFromBytes(buffer));
                putDataMapRequest.getDataMap().putString(Cc.KEY_FILE_PATH_ON_PHONE, imagePath);
                putDataMapRequest.getDataMap().putInt(Cc.KEY_SENDED_COUNT, SendImagesManager.getInstance().getSentCount());
                putDataMapRequest.getDataMap().putInt(Cc.KEY_TOTAL_COUNT, SendImagesManager.getInstance().getTotalCount());
                putDataMapRequest.getDataMap().putLong(Cc.KEY_SEND_TIME, System.currentTimeMillis());
                WTSender.sendData(context, putDataMapRequest, sendDataCallback);
            } catch (IOException e) {
                e.printStackTrace();
                SendImagesManager.getInstance().onSendFailed(SendImagesManager.RESULT_CREATE_BITMAP_NULL);
                stopSelf();
            }
        }
    }

    private void sendFinish() {
        isSending = false;
        //通知手表全部发送完毕
        WTSender.sendMessage(context, Cc.PATH_SEND_PICS_FINISH, "", null);
        //通知管理类
        SendImagesManager.getInstance().onSendSuccess();
        //停止服务
        stopSelf();
    }

    private void sendFailed(@SendImagesManager.SendResult int result) {
        isSending = false;
        SendImagesManager.getInstance().onSendFailed(result);
        stopSelf();
    }

    /**
     * 更新常驻通知
     */
    private void updateOngoingNotify(String title, String msg, String ticker) {
        startForeground(NOTIFY_ON_GOING_ID, getSendingNotify(title, msg, null));
    }

    /**
     * 构造通知
     */
    private Notification getSendingNotify(String title, String msg, String ticker) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        PendingIntent cancelPendingIntent = PendingIntent.getBroadcast(context, 2,
                new Intent(ACTION_CANCEL), PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, Utils.NOTIFY_CHANNEL_ID_SEND);
        builder.setSmallIcon(R.drawable.ic_send)
                .setContentTitle(title)
                .setContentText(msg)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_cancel, getString(R.string.cancel), cancelPendingIntent)
                .setOnlyAlertOnce(true);
        if (!TextUtils.isEmpty(ticker))
            builder.setTicker(ticker);
        return builder.build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.notify_channel_name);
            String description = getString(R.string.notify_channel_description);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(Utils.NOTIFY_CHANNEL_ID_SEND, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null)
                notificationManager.createNotificationChannel(channel);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
