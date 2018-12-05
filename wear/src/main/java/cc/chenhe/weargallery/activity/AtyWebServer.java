package cc.chenhe.weargallery.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.mobvoi.android.common.MobvoiApiManager;

import java.io.IOException;
import java.lang.ref.WeakReference;

import cc.chenhe.weargallery.R;
import cc.chenhe.weargallery.WebServer;
import cc.chenhe.weargallery.ui.AlertDialog;
import cc.chenhe.weargallery.uilts.Logger;
import cc.chenhe.weargallery.uilts.ZxingUtils;

public class AtyWebServer extends WearableActivity implements View.OnClickListener {

    private static final int MESSAGE_CONNECTIVITY_TIMEOUT = 1;
    private static final int MESSAGE_ON_AVAILABLE = 2;
    private static final long NETWORK_CONNECTIVITY_TIMEOUT_MS = 10000;


    TextView tvConnectStatus;

    WebServer webServer = null;
    MyHandler handler = new MyHandler(this);
    ConnectivityManager connectivityManager = null;
    ConnectivityManager.NetworkCallback networkCallback = null;

    private WifiReceiver wifiReceiver = null;

    private class WifiReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            //wifi连接上与否
            if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (info.getState().equals(NetworkInfo.State.CONNECTED)) {
                    startServer();
                }
            }
        }
    }

    private static class MyHandler extends Handler {
        private WeakReference<AtyWebServer> wr;

        MyHandler(@NonNull AtyWebServer aty) {
            wr = new WeakReference<>(aty);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            AtyWebServer aty = wr.get();
            if (aty == null || aty.isFinishing())
                return;
            switch (msg.what) {
                case MESSAGE_CONNECTIVITY_TIMEOUT:
                    // 等待LAN超时
                    aty.setTimeout();
                    break;
                case MESSAGE_ON_AVAILABLE:
                    aty.startServer();
                    break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (wifiReceiver != null)
            unregisterReceiver(wifiReceiver);
        if (handler != null)
            handler.removeCallbacksAndMessages(null);
        if (webServer != null)
            webServer.stop();
        if (useConnectivityManager(connectivityManager)) {
            if (Build.VERSION.SDK_INT >= 23)
                connectivityManager.bindProcessToNetwork(null);
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.aty_web_server);
        tvConnectStatus = findViewById(R.id.tvConnectStatus);

        findViewById(R.id.layoutAddNetwork).setOnClickListener(this);

        setAmbientEnabled();
        checkNetwork();
    }

    private void setTimeout() {
        tvConnectStatus.setText(getString(R.string.server_timeout));
        findViewById(R.id.pbConnectNetwork).setVisibility(View.GONE);
    }

    /**
     * 判断是否使用 {@link ConnectivityManager}。在 ticwear 系统上不使用。
     */
    private boolean useConnectivityManager(ConnectivityManager manager) {
        MobvoiApiManager.ApiGroup group = MobvoiApiManager.getInstance().getGroup();
        return manager != null && group.equals(MobvoiApiManager.ApiGroup.GMS);
    }

    private void checkNetwork() {
        connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (useConnectivityManager(connectivityManager)) {
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    super.onAvailable(network);
                    handler.removeMessages(MESSAGE_CONNECTIVITY_TIMEOUT);
                    if (Build.VERSION.SDK_INT >= 23)
                        connectivityManager.bindProcessToNetwork(network);
                    handler.sendEmptyMessage(MESSAGE_ON_AVAILABLE);
                }
            };

            NetworkRequest request = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .build();
            connectivityManager.requestNetwork(request, networkCallback);
            handler.sendEmptyMessageDelayed(MESSAGE_CONNECTIVITY_TIMEOUT,
                    NETWORK_CONNECTIVITY_TIMEOUT_MS);
        } else {
            WifiManager wifiManager = (WifiManager) getApplicationContext().
                    getSystemService(Context.WIFI_SERVICE);
            if (wifiManager == null)
                return;
            if (!wifiManager.isWifiEnabled()) {
                // wifi未开启
                setTimeout();
            }

            IntentFilter filter = new IntentFilter();
            filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
            filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
            wifiReceiver = new WifiReceiver();
            registerReceiver(wifiReceiver, filter);
        }
    }

    private void startServer() {
        findViewById(R.id.layoutConnect).setVisibility(View.GONE);
        webServer = new WebServer(this);
        ViewStub vs = findViewById(R.id.viewStubServerRunning);
        if (vs != null) vs.inflate();

        TextView tvIp = findViewById(R.id.tvServerIp);
        ImageView ivQrCode = findViewById(R.id.ivServerQrCode);

        try {
            webServer.start();
            String addr = getLanIp() + ":" + webServer.getListeningPort();
            Logger.i("IP:" + addr);
            tvIp.setText(getString(R.string.server_ip, addr));
            ivQrCode.setVisibility(View.VISIBLE);
            ivQrCode.setImageBitmap(ZxingUtils.generateBitmap("http://" + addr, 300, 300));
            findViewById(R.id.ivServerHelp).setOnClickListener(this);
        } catch (IOException e) {
            e.printStackTrace();
            tvIp.setText(getString(R.string.server_failed));
            ivQrCode.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.layoutAddNetwork:
                try {
                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                } catch (Exception e) {
                    Toast.makeText(this, getString(R.string.server_start_wifi_setting_failed), Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.ivServerHelp:
                new AlertDialog.Builder(this)
                        .setTitle(R.string.tip)
                        .setMessage(R.string.server_help_msg)
                        .setShowPositiveButton(true)
                        .show();
                break;
        }
    }

    @Nullable
    private String getLanIp() {
        WifiManager wifiManager = (WifiManager) getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null || !wifiManager.isWifiEnabled())
            return null;
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ip = wifiInfo.getIpAddress();
        return (ip & 0xFF) + "." +
                ((ip >> 8) & 0xFF) + "." +
                ((ip >> 16) & 0xFF) + "." +
                (ip >> 24 & 0xFF);
    }
}
