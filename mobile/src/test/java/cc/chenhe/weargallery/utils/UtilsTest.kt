package cc.chenhe.weargallery.utils

import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class UtilsTest {

    @Test
    fun setColorAlpha() {
        // #FF000000 -> #AA000000
        (-16777216).setAlpha(170) shouldBeEqualTo (-1442840576)
    }
}
