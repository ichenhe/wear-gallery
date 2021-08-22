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

package cc.chenhe.weargallery.common.util

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import java.io.File
import java.util.*

class UtilsTest {

    @Nested
    inner class IsSameDay {
        private val tz: TimeZone = TimeZone.getDefault()

        @BeforeEach
        fun init() {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"))
        }

        @AfterEach
        fun clear() {
            TimeZone.setDefault(tz)
        }

        @Test
        fun same() {
            // 2020/01/01 00:00:00, 2020/01/01 23:59:59
            expectThat(isSameDay(1577808000000, 1577894399000)).isTrue()
        }

        @Test
        fun differentDay() {
            // 2020/01/01 23:59:59, 2020/01/02 00:00:00
            expectThat(isSameDay(1577894399000, 1577894400000)).isFalse()
        }

        @Test
        fun differentYear() {
            // 2020/01/02 00:00:00, 2019/01/02 00:00:00
            expectThat(isSameDay(1577894400000, 1546358400000)).isFalse()
        }
    }

    @Nested
    inner class GetFileName {
        @Test
        fun normal() {
            val f = File("sdcard/test/123.png")
            expectThat(f.path.fileName).isEqualTo(f.name)
        }

        @Test
        fun root() {
            val f = File("/123.png")
            expectThat(f.path.fileName).isEqualTo("123.png")
        }
    }

    @Nested
    inner class GetFilePath {
        @Test
        fun normal() {
            val f = File("sdcard/test/123.png")
            expectThat(f.path.filePath).isEqualTo(f.parent!!)
        }

        @Test
        fun root() {
            val f = File("/123.png")
            expectThat(f.path.filePath).isEqualTo(f.parent)
        }
    }
}