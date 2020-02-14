package cc.chenhe.weargallery.wearvision.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.core.view.ViewCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton

private const val MINIMIZE_TARGET_SCALE = 0.1f

/**
 * A [FloatingActionButton] that can animate to to mini size.
 *
 * Use [minimize] to minimize and [maximize] to maximize. Change the collapsed state will not affect the visibility
 * which means set `visibility` to `VISIBLE`  does not automatically maximize if you have collapse it before. But call
 * [show] dose restore the collapse state to maximum if necessary.
 *
 * You can also set a X/Y translation in the collapsed state by setting [minimizeTranslationX] and
 * [minimizeTranslationY].
 */
class MinimizableFloatingActionButton @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FloatingActionButton(context, attrs, defStyleAttr) {

    var minimizeTranslationX = 0f
    var minimizeTranslationY = 0f
    private val minimizeAnimDuration = resources.getInteger(android.R.integer.config_shortAnimTime)

    private enum class MinimizeState {
        Idle,
        Minimizing,
        Maximizing
    }

    private var minimizeState = MinimizeState.Idle

    private val isMinimizing get() = minimizeState == MinimizeState.Minimizing

    private val isMaximizing get() = minimizeState == MinimizeState.Maximizing

    private fun shouldAnimateVisibilityChange(): Boolean = ViewCompat.isLaidOut(this) && !isInEditMode


    /**
     * A convenience method to set [minimizeTranslationX] and [minimizeTranslationY] at the same time.
     */
    fun setMinimizeTranslation(x: Float, y: Float) {
        minimizeTranslationX = x
        minimizeTranslationY = y
    }

    override fun show() {
        if (visibility != View.VISIBLE) {
            // Not visible, just call super, anyway, it wii recover to the maximized state internally.
            translationX = 0f
            translationY = 0f
            super.show()
        } else {
            // It is visible now, let's recover to the maximized state if necessary.
            maximize()
        }
    }

    /**
     * Animate the button to visible and maximum state.
     *
     * If the button is already visible, [listener] will get no callback even the button needs to be restored to its
     * maximum state.
     */
    override fun show(listener: OnVisibilityChangedListener?) {
        if (visibility != View.VISIBLE) {
            // Not visible, just call super, anyway, it wii recover to the maximized state internally.
            super.show(listener)
        } else {
            // It is visible now, let's recover to the maximized state if necessary.
            maximize()
            // No need to call listener here to be consistent with `show` behavior.
            // (`show` does not notify callback when already displayed)
        }
    }

    fun minimize() {
        if (isMinimizing || scaleX == MINIMIZE_TARGET_SCALE) {
            // A minimize animation is in progress, or we're already minimum. Skip the call.
            return
        }
        if (shouldAnimateVisibilityChange()) {
            animate().cancel()
            animate()
                    .scaleX(MINIMIZE_TARGET_SCALE)
                    .scaleY(MINIMIZE_TARGET_SCALE)
                    .translationX(minimizeTranslationX)
                    .translationY(minimizeTranslationY)
                    .setDuration(minimizeAnimDuration.toLong())
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationStart(animation: Animator?) {
                            minimizeState = MinimizeState.Minimizing
                        }

                        override fun onAnimationEnd(animation: Animator?) {
                            minimizeState = MinimizeState.Idle
                            isClickable = false
                            imageAlpha = 0
                        }
                    })
        } else {
            // If the view isn't laid out, or we're in the editor, don't run the animation
            scaleX = MINIMIZE_TARGET_SCALE
            scaleY = MINIMIZE_TARGET_SCALE
            translationX = minimizeTranslationX
            translationY = minimizeTranslationY
            isClickable = false
            imageAlpha = 0
        }
    }

    fun maximize() {
        if (scaleX == 1f || isMaximizing) {
            return
        }
        if (shouldAnimateVisibilityChange()) {
            animate().cancel()
            animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .translationX(0f)
                    .translationY(0f)
                    .setDuration(minimizeAnimDuration.toLong())
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationStart(animation: Animator?) {
                            minimizeState = MinimizeState.Maximizing
                            isClickable = true
                            imageAlpha = 0xFF
                        }

                        override fun onAnimationEnd(animation: Animator?) {
                            minimizeState = MinimizeState.Idle
                        }
                    })
        } else {
            scaleX = 1f
            scaleY = 1f
            translationX = 0f
            translationY = 0f
            isClickable = true
            imageAlpha = 0xFF
        }
    }

}