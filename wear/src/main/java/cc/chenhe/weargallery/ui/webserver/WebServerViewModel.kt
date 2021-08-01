/**
 * Copyright (C) 2020 Chenhe
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cc.chenhe.weargallery.ui.webserver

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.*
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.WebServer
import cc.chenhe.weargallery.repository.ImageRepository
import cc.chenhe.weargallery.uilts.context
import cc.chenhe.weargallery.uilts.setIfNot
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.chenhe.lib.wearmsger.WM
import timber.log.Timber

const val TAG = "WebServerVM"

class WebServerViewModel(
    application: Application,
    private val repository: ImageRepository
) : AndroidViewModel(application) {

    companion object {
        const val NETWORK_STATE_TRYING = 1
        const val NETWORK_STATE_TIMEOUT = 2
        const val NETWORK_STATE_AVAILABLE = 3

        const val SERVER_STATE_STOP = 10
        const val SERVER_STATE_RUNNING = 11
        const val SERVER_STATE_FAIL = 12

        private const val NETWORK_CONNECTIVITY_TIMEOUT_MS: Long = 10000
    }

    // -------------------------------------------------------------------------
    // Connect
    // -------------------------------------------------------------------------
    private val _networkState = MutableLiveData(NETWORK_STATE_TRYING)
    val networkState: LiveData<Int> = _networkState

    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var waitNetworkJob: Job? = null

    private var wifiReceiver: WifiReceiver? = null

    // ------------------------------------------------------------------------
    // Server
    // ------------------------------------------------------------------------

    private var webServer: WebServer? = null
    private val _serverState = MutableLiveData(SERVER_STATE_STOP)
    val serverState: LiveData<Int> = _serverState
    private val _serverIp = MutableLiveData<String?>(null)

    /** 192.168.1.1:0000 */
    val serverIp: LiveData<String?> = _serverIp

    /**
     * Error message when [serverState] is [SERVER_STATE_FAIL].
     */
    var serverErrorMsg: String? = null
        private set


    override fun onCleared() {
        super.onCleared()
        webServer?.stop()
        connectivityManager?.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                bindProcessToNetwork(null)
            }
            networkCallback?.let { unregisterNetworkCallback(it) }
        }
        wifiReceiver?.let { wifiReceiver ->
            context.unregisterReceiver(wifiReceiver)
        }
    }

    fun checkNetwork() {
        waitNetworkJob?.cancel()
        _networkState.setIfNot(NETWORK_STATE_TRYING)
        connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (shouldUsdConnectivityManager()) {
            requestWifi(requireNotNull(connectivityManager))
        } else {
            checkWifi(connectivityManager)
        }
    }

    private fun requestWifi(connectivityManager: ConnectivityManager) {
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                waitNetworkJob?.cancel()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    connectivityManager.bindProcessToNetwork(network)
                }
                _networkState.setIfNot(NETWORK_STATE_AVAILABLE, true)
            }
        }
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        connectivityManager.requestNetwork(request, requireNotNull(networkCallback))
        waitNetworkJob = viewModelScope.launch {
            delay(NETWORK_CONNECTIVITY_TIMEOUT_MS)
            _networkState.setIfNot(NETWORK_STATE_TIMEOUT, true)
        }
    }

    @Suppress("DEPRECATION")
    private fun checkWifi(connectivityManager: ConnectivityManager?) {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        if (wifiManager == null || !wifiManager.isWifiEnabled) {
            _networkState.setIfNot(NETWORK_STATE_TIMEOUT)
        }
        connectivityManager?.activeNetworkInfo?.let {
            if (it.type == ConnectivityManager.TYPE_WIFI && it.isConnected) {
                _networkState.setIfNot(NETWORK_STATE_AVAILABLE)
            }
        }

        val filter = IntentFilter().apply {
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        }
        wifiReceiver = WifiReceiver().also { context.registerReceiver(it, filter) }
    }

    /**
     * Whether should we use [ConnectivityManager] API.
     */
    private fun shouldUsdConnectivityManager(): Boolean {
        // We only use it in wear os.
        return !WM.isTicwear()
    }

    @Suppress("DEPRECATION")
    private inner class WifiReceiver : BroadcastReceiver() {
        override fun onReceive(p0: Context?, intent: Intent?) {
            if (intent?.action == WifiManager.NETWORK_STATE_CHANGED_ACTION) {
                val info: NetworkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO)!!
                if (info.state == NetworkInfo.State.CONNECTED) {
                    _networkState.setIfNot(NETWORK_STATE_AVAILABLE)
                } else {
                    _networkState.setIfNot(NETWORK_STATE_TIMEOUT)
                }
            }
        }
    }

    fun startServer() {
        stopServer()
        _serverIp.setIfNot(null)
        webServer = WebServer(context, repository).also { server ->
            try {
                server.start()
                _serverState.setIfNot(SERVER_STATE_RUNNING)
                val ip = getLanIp()?.let { it + ":" + server.listeningPort }
                _serverIp.setIfNot(ip)
                Timber.tag(TAG).i("Web server start: $ip")
            } catch (e: Exception) {
                Timber.tag(TAG).i("Web server start failed.")
                serverErrorMsg =
                    context.getString(R.string.server_failed, e.message) // must set message first
                _serverState.setIfNot(SERVER_STATE_FAIL)
                e.printStackTrace()
            }
        }
    }

    fun stopServer() {
        webServer?.apply {
            stop()
            _serverState.setIfNot(SERVER_STATE_STOP)
        }
    }

    private fun getLanIp(): String? {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        if (wifiManager == null || !wifiManager.isWifiEnabled) {
            return null
        }
        val ip = wifiManager.connectionInfo.ipAddress
        return (ip and 0xFF).toString() + "." +
                (ip shr 8 and 0xFF) + "." +
                (ip shr 16 and 0xFF) + "." +
                (ip shr 24 and 0xFF)
    }

    fun getServerStateMsg(serverState: Int): String? {
        return when (serverState) {
            SERVER_STATE_RUNNING -> context.getString(R.string.server_state_running)
            SERVER_STATE_STOP -> context.getString(R.string.server_state_stop)
            SERVER_STATE_FAIL -> context.getString(R.string.server_state_fail)
            else -> null
        }
    }

    fun getServerStateColor(serverState: Int): Int {
        return when (serverState) {
            SERVER_STATE_RUNNING -> ContextCompat.getColor(context, R.color.wv_basic_green)
            else -> ContextCompat.getColor(context, R.color.wv_basic_red)
        }
    }
}