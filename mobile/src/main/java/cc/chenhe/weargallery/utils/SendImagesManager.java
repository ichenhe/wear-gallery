package cc.chenhe.weargallery.utils;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.DrawableRes;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.text.TextUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

import cc.chenhe.weargallery.MainActivity;
import cc.chenhe.weargallery.R;
import cc.chenhe.weargallery.SendPicsService;

/**
 * 批量发送图片管理类
 * Created by 晨鹤 on 2016/12/22.
 */

public class SendImagesManager extends Observable {

    public static final int STATE_SEND_ITEM_START = 0;
    public static final int STATE_SEND_ITEM_SUCCESS = 1;

    @IntDef({STATE_SEND_ITEM_START, STATE_SEND_ITEM_SUCCESS})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SendStatus {
    }

    public static final int RESULT_CREATE_BITMAP_NULL = -1;
    public static final int RESULT_SEND_DATA_FAIL = -2;
    public static final int RESULT_SEND_DATA_TIME_OUT = -3;//手表迟迟没有确认接收

    @IntDef({RESULT_CREATE_BITMAP_NULL, RESULT_SEND_DATA_FAIL, RESULT_SEND_DATA_TIME_OUT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SendResult {
    }

    /*instance*/
    private static SendImagesManager instance;

    /*监听器集合*/
    private List<OnSendStateChangedListener> sendListeners;

    /*待发送队列*/
    private List<String> images;
    /*正在发送的图片*/
    private String sendingImage;
    /*总图片数=待发送+正在发送+已发送*/
    private int count;
    /*是否需要显示结果通知栏*/
    private boolean showResultNotification = false;
    private Context context;
    /*用于接收通知栏取消发送按钮事件*/
    private CancelReceiver receiver;

    private static final int NOTIFY_COMMON_ID = 1;

    public String getSendingImage() {
        return sendingImage;
    }

    public int getTotalCount() {
        return count;
    }

    public boolean isShowResultNotification() {
        return showResultNotification;
    }

    public void setShowResultNotification(boolean showResultNotification) {
        this.showResultNotification = showResultNotification;
    }

    /**
     * 获取已发送图片数（包括正在发送的）
     *
     * @return 已发送图片数
     */
    public int getSentCount() {
        return count - images.size();
    }

    /**
     * 释放内存，重置队列
     */
    private void release() {
        images.clear();
        sendingImage = null;
        count = 0;
    }

    public static SendImagesManager getInstance() {

        if (instance == null) {
            synchronized (SendImagesManager.class) {
                if (instance == null) {
                    instance = new SendImagesManager();
                }
            }
        }
        return instance;
    }

    private SendImagesManager() {
        images = new ArrayList<>();
        sendListeners = new ArrayList<>();
    }

    /**
     * 添加监听器
     */
    public void addSendListener(@NonNull OnSendStateChangedListener listener) {
        if (!sendListeners.contains(listener))
            sendListeners.add(listener);
    }

    /**
     * 移除监听器
     */
    public void removeSendListener(@Nullable OnSendStateChangedListener listener) {
        if (listener != null)
            sendListeners.remove(listener);
    }


    /**
     * 启动服务并开始发送，可以重复调用
     */
    public void startSend(Context context) {
        this.context = context.getApplicationContext();
        registerReceiver();
        //Service内部有防重复启动判断
        context.startService(new Intent(context.getApplicationContext(), SendPicsService.class));
    }

    public void cancel() {
        unregisterReceiver();
        context.stopService(new Intent(context.getApplicationContext(), SendPicsService.class));
        for (OnSendStateChangedListener listener : sendListeners)
            listener.onCancel(count - images.size() - 1, count);
        this.release();
    }

    private void registerReceiver() {
        if (receiver == null) {
            receiver = new CancelReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(SendPicsService.ACTION_CANCEL);
            this.context.registerReceiver(receiver, intentFilter);
        }
    }

    private void unregisterReceiver() {
        if (receiver != null) {
            this.context.unregisterReceiver(receiver);
            receiver = null;
        }
    }

    /**
     * 添加图片到队列
     *
     * @param images 图片路径
     */
    public void addImages(List<String> images) {
        this.images.addAll(images);
        count += images.size();
        for (int i = 0; i < sendListeners.size(); i++) {
            sendListeners.get(i).onQueueChanged();
        }
    }

    /**
     * 添加图片到队列
     *
     * @param image 图片路径
     */
    public void addImage(String image) {
        this.images.add(image);
        count++;
        for (int i = 0; i < sendListeners.size(); i++) {
            sendListeners.get(i).onQueueChanged();
        }
    }

    /**
     * 获取下一个图片的路径以便发送。
     * 仅供service发送时调用。
     *
     * @return 图片绝对路径
     */
    @Nullable
    public String getNextImagePathToSend() {
        if (images.size() == 0)
            return null;
        sendingImage = images.get(0);
        images.remove(0);
        return sendingImage;
    }

    /**
     * 当发送失败时由service调用
     */
    public void onSendFailed(int resultCode) {
        unregisterReceiver();
        if (isShowResultNotification())
            showNotification(context.getString(R.string.notify_send_images_failed_title),
                    context.getString(R.string.notify_send_images_failed_content,
                            getResultDescribe(context, resultCode), count - images.size() - 1),
                    context.getString(R.string.notify_send_images_failed_title),
                    R.drawable.ic_error);
        for (int i = 0; i < sendListeners.size(); i++) {
            sendListeners.get(i).onSendFailed(count - images.size() - 1, count, sendingImage, resultCode);
        }
        release();
    }

    /**
     * 当成功发送完毕时由service调用
     */
    public void onSendSuccess() {
        unregisterReceiver();
        if (isShowResultNotification())
            showNotification(context.getString(R.string.notify_send_images_success_title),
                    context.getString(R.string.notify_send_images_success_content,
                            SendImagesManager.getInstance().getSentCount()),
                    context.getString(R.string.notify_send_images_success_title),
                    R.drawable.ic_done);
        for (int i = 0; i < sendListeners.size(); i++) {
            sendListeners.get(i).onSendSuccess(count);
        }
        release();
    }

    /**
     * 当成单个图片发送状态改变时由service调用
     */
    public void onItemSendStateChanged(@SendStatus int stateCode) {
        for (int i = 0; i < sendListeners.size(); i++) {
            sendListeners.get(i).onItemSendStateChanged(stateCode);
        }
    }

    /**
     * 获取结果码的描述，带标点。
     *
     * @param resultCode 结果码
     * @return 描述文字
     */
    public static String getResultDescribe(Context context, @SendResult int resultCode) {
        switch (resultCode) {
            case RESULT_CREATE_BITMAP_NULL:
                return context.getString(R.string.send_images_create_bitmap_null);
            case RESULT_SEND_DATA_FAIL:
                return context.getString(R.string.send_images_send_data_fail);
            case RESULT_SEND_DATA_TIME_OUT:
                return context.getString(R.string.send_images_send_data_time_out);
            default:
                return context.getString(R.string.send_images_unknown_error);
        }
    }

    /**
     * 发出普通通知
     */
    private void showNotification(String title, String msg, String ticker, @DrawableRes int icon) {
        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        nm.notify(NOTIFY_COMMON_ID, getNotify(title, msg, ticker, icon));
    }

    /**
     * 构造通知
     */
    private Notification getNotify(String title, String msg, String ticker, @DrawableRes int icon) {
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, Utils.NOTIFY_CHANNEL_ID_SEND);
        builder.setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(msg)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        if (!TextUtils.isEmpty(ticker))
            builder.setTicker(ticker);
        return builder.build();
    }

    private class CancelReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (SendPicsService.ACTION_CANCEL.equals(intent.getAction())) {
                cancel();
            }
        }
    }

    /**
     * 发送状态监听器
     */
    public interface OnSendStateChangedListener {
        void onSendSuccess(int totalCount);

        void onSendFailed(int successCount, int totalCount, String sendingImage, int resultCode);

        void onItemSendStateChanged(int stateCode);

        void onQueueChanged();

        void onCancel(int successCount, int totalCount);
    }


}
