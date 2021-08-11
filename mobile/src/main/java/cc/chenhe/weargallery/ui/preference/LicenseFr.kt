package cc.chenhe.weargallery.ui.preference

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.RecyclerView
import cc.chenhe.weargallery.common.ui.BaseViewHolder
import cc.chenhe.weargallery.databinding.FrLicenseBinding
import cc.chenhe.weargallery.databinding.RvItemLicenseBinding

class LicenseFr : Fragment() {

    companion object {
        private const val MIT = "MIT License"
        private const val APACHE_2 = "Apache Software License 2.0"
    }

    private lateinit var binding: FrLicenseBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FrLicenseBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val navController = findNavController()
        val conf = AppBarConfiguration(navController.graph)
        binding.toolbar.setupWithNavController(navController, conf)

        val licenses = listOf(
            License("Android Open Source Project", "AOSP", APACHE_2, "https://source.android.com/"),
            License("Compressor", "zetbaitsu", MIT, "https://github.com/zetbaitsu/Compressor"),
            License("Glide", "bumptech", "Custom", "https://github.com/bumptech/glide"),
            License("Koin", "Koin", APACHE_2, "https://github.com/InsertKoinIO/koin"),
            License("Kotlin", "kotlinlang", APACHE_2, "https://github.com/Kotlin/"),
            License(
                "material-intro",
                "heinrichreimer",
                MIT,
                "https://github.com/heinrichreimer/material-intro"
            ),
            License("Moshi", "square", APACHE_2, "https://github.com/square/moshi"),
            License(
                "subsampling-scale-image-view",
                "davemorrissey",
                APACHE_2,
                "https://github.com/davemorrissey/subsampling-scale-image-view"
            ),
            License("Timber", "JakeWharton", APACHE_2, "https://github.com/JakeWharton/timber"),
            License("Wear-Msger", "Chenhe", MIT, "https://github.com/ichenhe/Wear-Msger"),
        )
        binding.list.adapter = Adapter(licenses)
    }

    private inner class Adapter(private val list: List<License>) :
        RecyclerView.Adapter<BaseViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
            return BaseViewHolder(
                RvItemLicenseBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                ).root
            )
        }

        override fun getItemCount(): Int = list.size

        override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
            val l1 = list[position].name + " - " + list[position].author
            val l2 = list[position].type + "\n" + list[position].url
            holder.setText(android.R.id.text1, l1)
            holder.setText(android.R.id.text2, l2)

            holder.itemView.setOnClickListener {
                val url = list[holder.bindingAdapterPosition].url
                val intent = Intent().apply {
                    action = Intent.ACTION_VIEW
                    data = Uri.parse(url)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            }
        }
    }

    private data class License(
        val name: String,
        val author: String,
        val type: String,
        val url: String
    )
}