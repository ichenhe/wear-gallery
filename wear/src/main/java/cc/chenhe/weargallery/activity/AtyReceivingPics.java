package cc.chenhe.weargallery.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.TextView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;

import cc.chenhe.lib.weartools.activity.WTActivity;
import cc.chenhe.weargallery.MobileListenerService;
import cc.chenhe.weargallery.R;
import cc.chenhe.weargallery.bean.eventmsg.LocalImageChangedMsg;

/**
 * 接收图片时显示
 * Created by 晨鹤 on 2016/6/16.
 */
public class AtyReceivingPics extends WTActivity {
    MyReceiver receiver = new MyReceiver();
    TextView textView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.aty_receiving_pics);
        textView = findViewById(R.id.tv);

        IntentFilter filter = new IntentFilter();
        filter.addAction(MobileListenerService.ACTION_RECEIVE_IMAGES_FAILED);
        filter.addAction(MobileListenerService.ACTION_RECEIVE_IMAGES_SUCCESS);
        filter.addAction(MobileListenerService.ACTION_RECEIVE_IMAGES_ITEM);
        filter.addAction(MobileListenerService.ACTION_RECEIVE_IMAGES_CANCEL);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        String count = getIntent().getIntExtra("sendedCount", 0) + "/" + getIntent().getIntExtra("totalCount", 0);
        textView.setText(getString(R.string.receiving_pics_text, count));
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        super.onDestroy();
    }

    class MyReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            switch (intent.getAction()) {
                case MobileListenerService.ACTION_RECEIVE_IMAGES_SUCCESS:
                    Toast.makeText(context, R.string.receiving_pics_success, Toast.LENGTH_SHORT).show();
                    finish();
                    break;
                case MobileListenerService.ACTION_RECEIVE_IMAGES_FAILED:
                    if (intent.getIntExtra("resultCode", -1) == MobileListenerService.FAILED_SEND_RECEIPT_MESSAGE)
                        Toast.makeText(context, R.string.receiving_pics_failed_send_receipt_msg, Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(context, R.string.receiving_pics_failed_wait_next_data_time_out, Toast.LENGTH_SHORT).show();
                    finish();
                    break;
                case MobileListenerService.ACTION_RECEIVE_IMAGES_ITEM:
                    String count = intent.getIntExtra("sendedCount", 0) + "/" + intent.getIntExtra("totalCount", 0);
                    textView.setText(getString(R.string.receiving_pics_text, count));
                    EventBus.getDefault().post(new LocalImageChangedMsg());
                    break;
                case MobileListenerService.ACTION_RECEIVE_IMAGES_CANCEL:
                    Toast.makeText(context, R.string.receiving_pics_cancel, Toast.LENGTH_SHORT).show();
                    finish();
                    break;
            }
        }
    }
}
