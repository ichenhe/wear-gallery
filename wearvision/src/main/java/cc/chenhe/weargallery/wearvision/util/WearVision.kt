package cc.chenhe.weargallery.wearvision.util

import android.content.Context
import android.os.Build

object WearVision {

    /**
     * Return whether the screen has a round shape.
     *
     * **Notice:** The method always return `true` if API level is below [Build.VERSION_CODES.M]. Generally all modern
     * wear os equipment should not trigger this rollback. But `Ticwear` of Mobvoi is an exception and all devices run
     * it have a round screen.
     *
     * @return `true` if the screen is rounded, false otherwise.
     */
    fun isScreenRound(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.resources.configuration.isScreenRound
        } else {
            true
        }
    }

}