package cc.chenhe.weargallery.repo

import android.content.Context
import cc.chenhe.weargallery.common.bean.CheckUpdateResp
import cc.chenhe.weargallery.common.util.CHECK_UPDATE_INTERVAL
import cc.chenhe.weargallery.common.util.NetUtil
import cc.chenhe.weargallery.common.util.getVersionCode
import cc.chenhe.weargallery.utils.lastCheckUpdateTime
import com.squareup.moshi.Moshi

class CheckUpdateRepo(context: Context, private val moshi: Moshi) {

    private val ctx = context.applicationContext

    /**
     * 检查更新。若最近检查过则视为无需更新。
     * @return 新版属性，若无需更新则返回 null。
     */
    suspend fun checkUpdate(): CheckUpdateResp? {
        if (System.currentTimeMillis() / 1000 - lastCheckUpdateTime(ctx) < CHECK_UPDATE_INTERVAL) {
            return null
        }
        val resp = NetUtil.checkUpdate(moshi) ?: return null
        val hasNewVersion = (resp.mobile?.latest?.code ?: 0) > getVersionCode(ctx)
        if (!hasNewVersion) {
            lastCheckUpdateTime(ctx, System.currentTimeMillis() / 1000)
        }
        return if (hasNewVersion) resp else null
    }
}