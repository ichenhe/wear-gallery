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

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.Test
import java.io.File

class UtilsTest {

    @Test
    fun isSameDay_same() {
        // 2020/01/01 00:00:00, 2020/01/01 23:59:59
        isSameDay(1577808000000, 1577894399000).shouldBeTrue()
    }

    @Test
    fun isSameDay_differentDay() {
        // 2020/01/01 23:59:59, 2020/01/02 00:00:00
        isSameDay(1577894399000, 1577894400000).shouldBeFalse()
    }

    @Test
    fun isSameDay_differentYear() {
        // 2020/01/02 00:00:00, 2019/01/02 00:00:00
        isSameDay(1577894400000, 1546358400000).shouldBeFalse()
    }

    @Test
    fun getFileName_normal() {
        val f = File("sdcard/test/123.png")
        f.path.fileName shouldBeEqualTo f.name
    }

    @Test
    fun getFileName_root() {
        val f = File("/123.png")
        f.path.fileName shouldBeEqualTo "123.png"
    }

    @Test
    fun getFilePath_normal() {
        val f = File("sdcard/test/123.png")
        f.path.filePath shouldBeEqualTo f.parent!!
    }

    @Test
    fun getFilePath_root() {
        val f = File("/123.png")
        f.path.filePath shouldBeEqualTo f.parent
    }
}