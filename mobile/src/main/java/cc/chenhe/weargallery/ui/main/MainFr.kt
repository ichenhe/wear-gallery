package cc.chenhe.weargallery.ui.main

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cc.chenhe.weargallery.BuildConfig
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.common.util.*
import cc.chenhe.weargallery.databinding.FrMainBinding
import cc.chenhe.weargallery.databinding.ItemCardBinding
import cc.chenhe.weargallery.ui.legacy.LegacyAty
import cc.chenhe.weargallery.utils.lastCheckUpdateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import timber.log.Timber

class MainFr : Fragment() {

    private lateinit var binding: FrMainBinding
    private lateinit var adapter: CardAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FrMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.legacy.setOnClickListener {
            startActivity(Intent(requireContext(), LegacyAty::class.java))
        }
        binding.preference.setOnClickListener {
            findNavController().navigate(MainFrDirections.actionMainFrToPreferenceFr())
        }

        adapter = CardAdapter(requireContext())
        binding.cardList.adapter = adapter
        val cards = mutableListOf(
            Card(
                getString(R.string.card_intro), getString(R.string.card_intro_msg),
                R.drawable.ic_card_tip, R.color.card_bg_info, R.color.card_accent_info,
            )
        )
        if (isTicHelperInstalled(requireContext())) {
            cards.add(
                0, Card(
                    getString(R.string.card_tic), getString(R.string.card_tic_msg),
                    R.drawable.ic_card_warn, R.color.card_bg_warn, R.color.card_accent_warn,
                )
            )
        }
        adapter.submitList(cards)
        checkUpdate()
    }

    private fun isTicHelperInstalled(context: Context): Boolean {
        val names = arrayOf(
            "com.mobvoi.companion.aw",
            "com.mobvoi.baiding",
        )
        val pm = context.applicationContext.packageManager
        names.forEach {
            try {
                pm.getPackageInfo(it, 0)
                return true
            } catch (e: PackageManager.NameNotFoundException) {
                Timber.tag("MainFr").d("Fail to check the package $it")
            }
        }
        return false
    }

    private fun checkUpdate() {
        if (System.currentTimeMillis() / 1000 - lastCheckUpdateTime(requireContext()) < CHECK_UPDATE_INTERVAL) {
            return
        }
        lifecycleScope.launch(Dispatchers.IO) {
            val resp = NetUtil.checkUpdate(get()) ?: return@launch
            if (resp.mobile?.latest?.code ?: 0 > getVersionCode(requireContext())) {
                // new version
                val card = Card(
                    getString(R.string.card_update),
                    getString(R.string.card_update_msg, resp.mobile!!.latest!!.name ?: ""),
                    R.drawable.ic_card_update,
                    R.color.card_bg_success,
                    R.color.card_accent_success,
                    positive = getString(R.string.card_update_positive),
                    tag = "UPDATE",
                    obj = resp.mobile!!.url
                )
                adapter.getCurrentData().toMutableList().also {
                    it.add(0, card)
                    adapter.submitList(it)
                }
            } else {
                lastCheckUpdateTime(requireContext(), System.currentTimeMillis() / 1000)
            }
        }
    }

    private class CardAdapter(private val context: Context) :
        ListAdapter<Card, CardAdapter.CardVH>(DiffCallback()) {

        fun getCurrentData(): List<Card> = currentList

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardVH {
            val b = ItemCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return CardVH(b)
        }

        override fun onBindViewHolder(holder: CardVH, position: Int) {
            val card = getItem(position)
            holder.setAccentColor(card.accentColor)
            holder.setIcon(card.icon)
            holder.binding.apply {
                title.text = card.title
                message.text = card.message
                positiveBtn.isVisible = card.positive != null
                positiveBtn.text = card.positive
                negativeBtn.isVisible = card.negative != null
                negativeBtn.text = card.negative
            }
            holder.binding.root.backgroundTintList =
                ColorStateList.valueOf(context.getColor(card.bgColor))
            if (card.tag == "UPDATE") {
                holder.binding.positiveBtn.setOnClickListener {
                    if (BuildConfig.IS_GP) {
                        openMarket(context) {
                            openWithBrowser(context, card.obj as? String ?: GITHUB)
                        }
                    } else {
                        openWithBrowser(context, card.obj as? String ?: GITHUB)
                    }
                }
            }
        }

        private inner class CardVH(val binding: ItemCardBinding) :
            RecyclerView.ViewHolder(binding.root) {

            fun setIcon(@DrawableRes icon: Int) {
                if (icon == 0) {
                    binding.icon.isVisible = false
                } else {
                    binding.icon.setImageResource(icon)
                    binding.icon.isVisible = true
                }
            }

            fun setAccentColor(@ColorRes color: Int) {
                if (color != 0) {
                    val c = context.getColor(color)
                    ImageViewCompat.setImageTintList(binding.icon, ColorStateList.valueOf(c))
                    binding.positiveBtn.setTextColor(c)
                    binding.negativeBtn.setTextColor(c)
                }
            }
        }
    }


    private class DiffCallback : DiffUtil.ItemCallback<Card>() {
        override fun areItemsTheSame(oldItem: Card, newItem: Card): Boolean = oldItem == newItem

        override fun areContentsTheSame(oldItem: Card, newItem: Card): Boolean = oldItem == newItem
    }

    private data class Card(
        val title: String?,
        val message: String?,
        @DrawableRes val icon: Int,
        @ColorRes val bgColor: Int,
        @ColorRes val accentColor: Int,
        val positive: String? = null,
        val negative: String? = null,
        val tag: String? = null,
        val obj: Any? = null
    )
}