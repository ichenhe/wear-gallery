package cc.chenhe.weargallery.common.util

import cc.chenhe.weargallery.common.bean.CheckUpdateResp
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object NetUtil {
    class NetworkException(val responseCode: Int, message: String?, cause: Throwable? = null) :
        RuntimeException(message, cause)

    /**
     * @return null if encounter any errors.
     */
    @Suppress("BlockingMethodInNonBlockingContext") // IO dispatchers
    suspend fun checkUpdate(
        moshi: Moshi
    ): CheckUpdateResp? = withContext(Dispatchers.IO) {
        try {
            val s = getString(API_VER) ?: return@withContext null
            moshi.adapter(CheckUpdateResp::class.java).fromJson(s)
        } catch (e: NetworkException) {
            // ignore
            null
        } catch (e: JsonDataException) {
            // ignore
            null
        }
    }

    /**
     * @throws NetworkException
     */
    @Suppress("BlockingMethodInNonBlockingContext") // IO Dispatcher
    private suspend fun getString(url: String): String? = withContext(Dispatchers.IO) {
        var code = 0
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                readTimeout = 5000
                connectTimeout = 5000
            }
            code = conn.responseCode
            if (code !in 200 until 300) {
                throw NetworkException(code, conn.responseMessage)
            }
            conn.inputStream?.use {
                String(it.readBytes())
            }

        } catch (e: NetworkException) {
            throw e
        } catch (e: Exception) {
            throw NetworkException(code, null, e)
        } finally {
            conn?.disconnect()
        }
    }
}