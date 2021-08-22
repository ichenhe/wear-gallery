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

package cc.chenhe.weargallery.uilts.diskcache

import okio.buffer
import okio.sink
import okio.source
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import java.io.File

private const val APP_VERSION = 1L
private const val MAX_SIZE = 1000L //1000B

private const val KEY = "test_key"
private const val DATA = "Test Data"

class DiskLruCacheTest {

    private lateinit var cacheFile: File
    private lateinit var cache: DiskLruCache

    @BeforeEach
    fun setup() {
        cacheFile = File(System.getProperty("java.io.tmpdir"), "wg_cache_test")
        clean()
        cache = DiskLruCache.open(cacheFile, APP_VERSION, MAX_SIZE)
    }

    @AfterEach
    fun clean() {
        if (cacheFile.isDirectory && cacheFile.exists()) {
            cacheFile.deleteRecursively()
        }
    }

    private fun writeCache(key: String, data: String, commit: Boolean = true): DiskLruCache.Editor {
        val editor = cache.edit(key)!!
        editor.getTargetFile().sink().use {
            it.buffer().use { bs ->
                bs.writeUtf8(data)
            }
        }
        if (commit) {
            editor.commit()
        }
        return editor
    }

    private fun writeCacheBytes(key: String, size: Int) {
        val editor = cache.edit(key)!!
        editor.getTargetFile().sink().use {
            it.buffer().use { bs ->
                bs.write(ByteArray(size))
            }
        }
        editor.commit()
    }

    @Test
    fun getFile_noCache_null() {
        expectThat(cache.getFile("no")).isNull()
    }

    @Test
    fun edit_createCache() {
        writeCache(KEY, DATA)

        val cached = cache.getFile(KEY)
        expectThat(cached).isNotNull()
        cached!!.source().use {
            it.buffer().use { bs ->
                expectThat(bs.readUtf8()).isEqualTo(DATA)
            }
        }
    }

    @Test
    fun edit_abort() {
        writeCache(KEY, DATA, false).abort()
        expectThat(cache.getFile(KEY)).isNull()
    }

    @Test
    fun remove_alreadyExist() {
        writeCache(KEY, DATA)
        cache.remove(KEY)
        expectThat(cache.getFile(KEY)).isNull()
    }

    @Test
    fun remove_notExist() {
        cache.remove(KEY)
        expectThat(cache.getFile(KEY)).isNull()
    }

    @Test
    fun edit_lru_constant() {
        lru(null)
    }

    @Test
    fun edit_lru_reload() {
        lru {
            cache = DiskLruCache.open(cacheFile, APP_VERSION, MAX_SIZE)
        }
    }

    private fun lru(beforeOverride: (() -> Unit)?) {
        // 5*100 = 500B
        val size = 100L
        for (i in 0..4) {
            writeCacheBytes(i.toString(), size.toInt())
        }
        for (i in 0..4) {
            val cached = cache.getFile(i.toString())
            expectThat(cached).isNotNull()
            expectThat(cached!!.length()).isEqualTo(size)
        }

        cache.getFile("2")
        cache.getFile("4")
        cache.getFile("1")
        beforeOverride?.invoke()
        writeCacheBytes("big", 800)
        Thread.sleep(50)
        // queue: [0,3,2],4,1,big
        for (i in arrayOf(0, 3, 2)) {
            expectThat(cache.getFile(i.toString())).isNull()
        }
        for (i in arrayOf(4, 1)) {
            cache.getFile(i.toString()).let {
                expect {
                    that(it).isNotNull()
                    that(it!!.length()).isEqualTo(size)
                }
            }
        }
        cache.getFile("big").let {
            expectThat(it).isNotNull()
            expectThat(it!!.length()).isEqualTo(800L)
        }
    }
}