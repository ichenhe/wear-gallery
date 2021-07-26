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

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.databinding.FrWebServerBinding
import cc.chenhe.weargallery.databinding.FrWebServerRunningBinding
import cc.chenhe.weargallery.ui.common.SwipeDismissFr
import cc.chenhe.weargallery.uilts.ZxingUtils
import cc.chenhe.weargallery.uilts.toast
import me.chenhe.wearvision.dialog.AlertDialog
import org.koin.androidx.viewmodel.ext.android.viewModel


class WebServerFr : SwipeDismissFr() {

    private lateinit var binding: FrWebServerBinding
    private val model: WebServerViewModel by viewModel()

    override fun createView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?)
            : ViewBinding {
        return FrWebServerBinding.inflate(inflater, container, false).also {
            binding = it
            it.lifecycleOwner = viewLifecycleOwner
            it.model = model
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.connectLayout.addNetworkLayout.setOnClickListener {
            // Add network manually
            try {
                startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            } catch (e: Exception) {
                toast(R.string.server_start_wifi_setting_failed)
            }
        }

        model.networkState.observe(viewLifecycleOwner) { networkState ->
            when (networkState) {
                WebServerViewModel.NETWORK_STATE_TRYING,
                WebServerViewModel.NETWORK_STATE_TIMEOUT -> {
                    model.stopServer()
                }
                WebServerViewModel.NETWORK_STATE_AVAILABLE -> {
                    binding.serverRunningStub.viewStub?.inflate()
                    model.startServer()
                }
            }
        }

        model.serverIp.observe(viewLifecycleOwner) { serverIp ->
            (binding.serverRunningStub.binding as? FrWebServerRunningBinding)?.serverHelp?.setOnClickListener {
                AlertDialog(requireContext()).apply {
                    setTitle(R.string.tip)
                    setMessage(R.string.server_help_msg)
                    setPositiveButtonIcon(R.drawable.ic_dialog_confirm, null)
                }.show()
            }
            (binding.serverRunningStub.binding as? FrWebServerRunningBinding)?.serverQrCode?.let { serverQrCode ->
                if (serverIp.isNullOrEmpty()) {
                    serverQrCode.setImageBitmap(null)
                } else {
                    serverQrCode.setImageBitmap(ZxingUtils.generateBitmap("http://$serverIp", 300, 300))
                }
            }
        }

        model.checkNetwork()

    }

}