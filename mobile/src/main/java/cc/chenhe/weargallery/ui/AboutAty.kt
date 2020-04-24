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

package cc.chenhe.weargallery.ui

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import cc.chenhe.weargallery.BuildConfig
import cc.chenhe.weargallery.R
import com.alibaba.fastjson.JSON
import com.drakeet.about.*
import com.drakeet.about.extension.JsonConverter
import com.drakeet.about.extension.RecommendationLoaderDelegate
import com.drakeet.about.provided.GlideImageLoader

class AboutAty : AbsAboutActivity(), OnRecommendationClickedListener {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setImageLoader(GlideImageLoader())
        onRecommendationClickedListener = this
    }

    override fun onCreateHeader(icon: ImageView, slogan: TextView, version: TextView) {
        icon.visibility = View.GONE
        slogan.setText(R.string.about_slogan)
        version.text = BuildConfig.VERSION_NAME
    }

    override fun onItemsCreated(items: MutableList<Any>) {
        items.add(Category(getString(R.string.about_os_title)))
        items.add(Card(getString(R.string.about_os_content)))
        items.add(Category(getString(R.string.about_thank_title)))
        items.add(Card(getString(R.string.about_thank_content)))
        items.add(Category(getString(R.string.about_follow_title)))
        items.add(Card(getString(R.string.about_follow_content)))

        RecommendationLoaderDelegate.attach(this, items.size, object : JsonConverter {
            @Throws(Exception::class)
            override fun <T> fromJson(json: String, classOfT: Class<T>): T? {
                return JSON.parseObject(json, classOfT)
            }

            override fun <T> toJson(src: T?, classOfT: Class<T>): String {
                return JSON.toJSONString(src)
            }
        })

        items.add(Category(getString(R.string.about_licence)))
        items.add(License("Wear-Msger", "Chenhe", License.MIT, "https://github.com/liangchenhe55/Wear-Msger"))
        items.add(License("about-page", "drakeet", License.APACHE_2, "https://github.com/PureWriter/about-page"))
        items.add(License("Luban", "Curzibn", License.APACHE_2, "https://github.com/Curzibn/Luban"))
        items.add(License("glide", "bumptech", "Custom License", "https://github.com/bumptech/glide"))
        items.add(License("Subsampling Scale Image View", "davemorrissey", License.APACHE_2, "https://github.com/davemorrissey/subsampling-scale-image-view"))
        items.add(License("material-intro", "heinrichreimer", License.MIT, "https://github.com/heinrichreimer/material-intro"))
        items.add(License("Moshi", "square", License.APACHE_2, "https://github.com/square/moshi"))
        items.add(License("Koin", "Koin", License.APACHE_2, "https://github.com/InsertKoinIO/koin"))
        items.add(License("Android Open Source Project", "AOSP", License.APACHE_2, "https://source.android.com/"))
    }

    override fun onRecommendationClicked(itemView: View, recommendation: Recommendation): Boolean {
        if (recommendation.openWithGooglePlay) {
            openMarket(this, recommendation.packageName, recommendation.downloadUrl)
        } else {
            openWithBrowser(this, recommendation.downloadUrl)
        }
        return false
    }

    private fun openMarket(context: Context, targetPackage: String, defaultDownloadUrl: String) {
        try {
            val googlePlayIntent = context.packageManager.getLaunchIntentForPackage("com.android.vending")
            val comp = ComponentName("com.android.vending", "com.google.android.finsky.activities.LaunchUrlHandlerActivity")
            // noinspection ConstantConditions
            googlePlayIntent!!.component = comp
            googlePlayIntent.data = Uri.parse("market://details?id=$targetPackage")
            context.startActivity(googlePlayIntent)
        } catch (e: Throwable) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(defaultDownloadUrl)))
            e.printStackTrace()
        }
    }

    companion object {
        fun openWithBrowser(context: Context, url: String?) {
            try {
                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW, Uri.parse(url)),
                        context.getString(R.string.links_chooser_browser)))
            } catch (e: ActivityNotFoundException) {
            }
        }
    }
}