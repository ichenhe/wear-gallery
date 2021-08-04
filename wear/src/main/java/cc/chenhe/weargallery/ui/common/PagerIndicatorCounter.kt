package cc.chenhe.weargallery.ui.common

import android.content.Context
import android.view.View
import kotlinx.coroutines.*

/**
 * A util for hiding ViewPager indicator after a certain period of time.
 *
 * Usage:
 *
 * ```kotlin
 * indicatorCounter = PagerIndicatorCounter(requireContext(), lifecycleScope) { visible ->
 *      if (visible) indicatorCounter.fadeIn(tabLayout)
 *      else indicatorCounter.fadeOut(tabLayout)
 * }
 * indicatorCounter.resetVisibilityCountdown()
 * ```
 *
 * @param hideDelay Default waiting time(ms) before hide.
 */
class PagerIndicatorCounter(
    context: Context,
    private val scope: CoroutineScope,
    private val hideDelay: Long = 1000L,
    private val callback: (visible: Boolean) -> Unit
) {

    private val fadeAnimationDuration: Int =
        context.resources.getInteger(android.R.integer.config_shortAnimTime)

    private var hideJob: Job? = null

    private var fadingIn = false
    private var fadingOut = false

    /**
     * Set to visible and restart the timer.
     *
     * @param delay If greater than 0, use the given value, else use [hideDelay].
     */
    fun resetVisibilityCountdown(delay: Long = 0) {
        hideJob?.cancel()
        callback(true)
        hideJob = scope.launch(Dispatchers.Main) {
            delay(if (delay > 0) delay else hideDelay)
            callback(false)
        }
    }

    /**
     * Trigger a show event and cancel countdown job.
     */
    fun pin() {
        hideJob?.cancel()
        callback(true)
    }

    fun fadeIn(view: View) {
        if (view.visibility == View.VISIBLE && (view.alpha == 1f || fadingIn))
            return
        fadingIn = true
        fadingOut = false
        view.animation?.cancel()
        view.animate()
            .alpha(1f)
            .setDuration(fadeAnimationDuration.toLong())
            .withStartAction {
                view.visibility = View.VISIBLE
            }
            .withEndAction {
                fadingIn = false
            }
    }

    fun fadeOut(view: View) {
        if (view.visibility == View.GONE || fadingOut)
            return
        view.animation?.cancel()
        fadingIn = false
        fadingOut = true
        view.animate()
            .alpha(0f)
            .setDuration(fadeAnimationDuration.toLong())
            .withEndAction {
                view.visibility = View.GONE
                fadingOut = false
            }
    }
}