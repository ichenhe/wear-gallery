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

package cc.chenhe.weargallery.ui.main

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import cc.chenhe.weargallery.databinding.FrPagerBinding
import cc.chenhe.weargallery.ui.IntroduceAty
import cc.chenhe.weargallery.ui.UpgradingAty
import cc.chenhe.weargallery.uilts.ACTION_APPLICATION_UPGRADE_COMPLETE
import cc.chenhe.weargallery.uilts.NOTIFY_ID_PERMISSION
import org.koin.androidx.viewmodel.ext.android.viewModel

private const val REQUEST_INTRODUCE = 1
private const val REQUEST_UPGRADE = 2

class PagerFr : Fragment() {

    private lateinit var binding: FrPagerBinding
    private val model: PageViewModel by viewModel()

    private var upgradeReceiver: UpgradeReceiver? = null

    private lateinit var adapter: PagerAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FrPagerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!checkPermission()) {
            startActivityForResult(Intent(requireContext(), IntroduceAty::class.java), REQUEST_INTRODUCE)
        } else {
            checkUpgradeAndLoadData()
        }
    }

    private fun checkUpgradeAndLoadData() {
        val r = UpgradingAty.startIfNecessary(this, REQUEST_UPGRADE) {
            upgradeReceiver = UpgradeReceiver().also { receiver ->
                LocalBroadcastManager.getInstance(requireContext())
                        .registerReceiver(receiver, IntentFilter(ACTION_APPLICATION_UPGRADE_COMPLETE))
            }
        }
        if (!r) {
            // no need to upgrade
            loadFragments()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        upgradeReceiver?.let { LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(it) }
        upgradeReceiver = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_INTRODUCE) {
            if (checkPermission()) {
                checkUpgradeAndLoadData()
                context?.let { ctx -> NotificationManagerCompat.from(ctx).cancel(NOTIFY_ID_PERMISSION) }
            } else {
                activity?.finish()
            }
        } else if (requestCode == REQUEST_UPGRADE) {
            if (resultCode != Activity.RESULT_OK) {
                activity?.finish()
            }
        }
    }

    private fun loadFragments() {
        model.items.observe(viewLifecycleOwner) {
            if (binding.pager.adapter == null) {
                adapter = PagerAdapter()
                binding.pager.adapter = adapter
            } else {
                adapter.notifyDataSetChanged()
            }
        }
    }

    /**
     * @return Whether has permissions.
     */
    private fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    private inner class UpgradeReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_APPLICATION_UPGRADE_COMPLETE) {
                this@PagerFr.context?.let { ctx ->
                    LocalBroadcastManager.getInstance(ctx).unregisterReceiver(this)
                }
                loadFragments()
            }
        }
    }

    private inner class PagerAdapter : FragmentStateAdapter(this) {
        override fun getItemCount(): Int = model.size

        override fun createFragment(position: Int): Fragment {
            return model.createFragment(position) ?: throw IllegalArgumentException("Unexpected main pager index.")
        }

        override fun getItemId(position: Int): Long {
            return model.getId(position)
        }

        override fun containsItem(itemId: Long): Boolean {
            return model.contains(itemId)
        }
    }
}