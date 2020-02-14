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

package cc.chenhe.weargallery

import cc.chenhe.weargallery.common.jsonadapter.UriAdapter
import cc.chenhe.weargallery.db.MainDb
import cc.chenhe.weargallery.repository.ImageRepository
import cc.chenhe.weargallery.repository.RemoteImageRepository
import cc.chenhe.weargallery.ui.imagedetail.local.LocalImageDetailViewModel
import cc.chenhe.weargallery.ui.imagedetail.mobile.MobileImageDetailViewModel
import cc.chenhe.weargallery.ui.local.LocalImagesViewModel
import cc.chenhe.weargallery.ui.main.PageViewModel
import cc.chenhe.weargallery.ui.main.SharedViewModel
import cc.chenhe.weargallery.ui.pick.PickImageViewModel
import cc.chenhe.weargallery.ui.webserver.WebServerViewModel
import cc.chenhe.weargallery.uilts.diskcache.MobilePreviewCacheManager
import cc.chenhe.weargallery.watchface.PreferenceViewModel
import cc.chenhe.weargallery.watchface.style.TimeTextColorViewModel
import cc.chenhe.weargallery.watchface.style.TimeTextFormatViewModel
import com.squareup.moshi.Moshi
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

val appModule = module {

    viewModel { SharedViewModel(androidApplication(), get()) }
    viewModel { PageViewModel(androidApplication()) }
    viewModel { LocalImagesViewModel(androidApplication()) }
    viewModel { MobileImageDetailViewModel(androidApplication(), get()) }
    viewModel { LocalImageDetailViewModel(androidApplication()) }
    viewModel { WebServerViewModel(androidApplication(), get()) }
    viewModel { PickImageViewModel(androidApplication()) }
    viewModel { PreferenceViewModel(androidApplication()) }
    viewModel { TimeTextFormatViewModel(androidApplication()) }
    viewModel { TimeTextColorViewModel(androidApplication()) }

    factory { Moshi.Builder().add(UriAdapter()).build() }


    // ---------------------------------------------------------------------
    // Database
    // ---------------------------------------------------------------------

    single { MainDb.getInstance(androidApplication()) }
    single { get<MainDb>().remoteImageFolderDao() }
    single { get<MainDb>().remoteImageDao() }


    // ---------------------------------------------------------------------
    // Repository
    // ---------------------------------------------------------------------

    single { RemoteImageRepository(get(), get(), get(), get()) } bind ImageRepository::class


    // ---------------------------------------------------------------------
    // Others
    // ---------------------------------------------------------------------

    single { MobilePreviewCacheManager.getInstance(androidApplication()) }
}