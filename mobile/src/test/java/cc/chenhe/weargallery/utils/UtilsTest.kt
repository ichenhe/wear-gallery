package cc.chenhe.weargallery.utils

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class UtilsTest {

    @Test
    fun setColorAlpha() {
        // #FF000000 -> #AA000000
        expectThat((-16777216).setAlpha(170)).isEqualTo(-1442840576)
    }
}
