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

package cc.chenhe.weargallery.watchface.painter

import cc.chenhe.weargallery.uilts.*
import cc.chenhe.weargallery.watchface.ITimeHolder
import timber.log.Timber
import java.util.*
import kotlin.collections.HashMap

/**
 * Must call [onFrameStart] every time of each frame start to make sure update tags.
 */
internal class TagParser(
    private val timeHolder: ITimeHolder
) {
    companion object {
        private const val TAG = "TagParser"
        private const val N = 10 // max count of tags
        private const val SZ = 10 * N
        private const val ASCN = 127
    }

    private val calendar = timeHolder.calendar()

    private val keys: Array<String> = arrayOf(
        TIME_TAG_LINE, TIME_TAG_HOUR, TIME_TAG_MIN, TIME_TAG_COLON,
        TIME_TAG_YEAR, TIME_TAG_MONTH, TIME_TAG_DAY, TIME_TAG_SLASH
    )

    /**
     * The value of each tag.
     */
    private val values = HashMap<String, String>(N)
    private val builder = StringBuilder()

    private val trie = Array(SZ) { IntArray(ASCN) { 0 } }
    private val fail = IntArray(SZ) { 0 }
    private val end = IntArray(SZ) { 0 }
    private var tot = 1
    private val q = ArrayDeque<Int>()
    private var getFail = false

    init {
        initTags()
    }

    private fun initTags() {
        keys.forEach { insert(it) }
    }

    // ------------------------------------------------------------------------------------------
    // trie
    // ------------------------------------------------------------------------------------------

    private fun getFail() {
        getFail = true
        q.clear()
        trie[0].forEachIndexed { i, _ -> trie[0][i] = 1 }
        q.addLast(1)
        fail[1] = 0
        while (!q.isEmpty()) {
            val p = q.pop()
            for (i in 0 until ASCN) {
                if (trie[p][i] == 0) {
                    trie[p][i] = trie[fail[p]][i]
                } else {
                    q.addLast(trie[p][i])
                    fail[trie[p][i]] = trie[fail[p]][i]
                }
            }
        }
    }

    private fun StringBuilder.replace1(start: Int, num: Int, str: String) {
        replace(start, start + num, str)
    }

    private fun StringBuilder.sub(start: Int, num: Int): String {
        return substring(start, start + num)
    }

    private fun insert(tag: String) {
        var p = 1
        tag.forEach { c ->
            if (trie[p][c.code] == 0) {
                tot += 1
                trie[p][c.code] = tot
            }
            p = trie[p][c.code]
        }
        end[p] = tag.length
    }

    private fun replace(str: CharSequence): CharSequence {
        if (!getFail) {
            getFail()
        }
        var p = 1
        builder.clear()
        for (c in str) {
            builder.append(c)
            if (c.code !in 0..126) {
                continue // 忽略非 ascii 字符
            }
            var k = trie[p][c.code]
            while (k > 1) {
                // 匹配成功，找到一个模式串
                if (end[k] != 0) {
                    val last = builder.length - end[k]
                    builder.sub(last, end[k]).let { tag ->
                        if (!values.containsKey(tag)) {
                            // 更新标签
                            updateTag(tag)
                        }
                        // 将该模式串替换为对应的值
                        values[builder.sub(last, end[k])]?.let {
                            builder.replace1(last, end[k], it)
                        }
                    }

                    // 从前向后匹配模式串并替换，假设模式串中不存在一个是另一个的子串
                    p = 0 // 替换成功，返回字典树起始位值
                    break
                }
                k = fail[k] // 匹配失败，继续下一轮
            }
            p = trie[p][c.code] // 继续走
        }
        return builder
    }

    // -------------------------------------------------------------------------------------
    // API
    // -------------------------------------------------------------------------------------

    fun reset() {
        trie.forEachIndexed { i, v ->
            v.forEachIndexed { j, _ -> trie[i][j] = 0 }
        }
        fail.forEachIndexed { i, _ -> fail[i] = 0 }
        end.forEachIndexed { i, _ -> fail[i] = 0 }
        tot = 1
        q.clear()
        initTags()
        getFail = false
    }

    fun parseString(str: String?): String? {
        var cs: CharSequence = str ?: return null
        cs = replace(cs)
        return cs.toString()
    }

    /**
     * This function must be called every time of each frame start to make sure update tags.
     */
    fun onFrameStart() {
        values.clear()
    }

    private fun updateTag(tag: String) {
        val r: Any? = when (tag) {
            TIME_TAG_HOUR -> timeHolder.getLeadingZero(timeHolder.getHourAuto(timeHolder.is24HourFormat()))
            TIME_TAG_MIN -> timeHolder.getLeadingZero(calendar.get(Calendar.MINUTE))
            TIME_TAG_YEAR -> calendar.get(Calendar.YEAR)
            TIME_TAG_MONTH -> timeHolder.getLeadingZero(calendar.get(Calendar.MONTH) + 1)
            TIME_TAG_DAY -> timeHolder.getLeadingZero(calendar.get(Calendar.DAY_OF_MONTH))

            TIME_TAG_LINE -> "\n"
            TIME_TAG_COLON -> ":"
            TIME_TAG_SLASH -> "/"
            else -> null
        }
        values[tag] = r?.toString() ?: run {
            Timber.tag(TAG).w("Not find value for tag <$tag>")
            ""
        }
    }

}