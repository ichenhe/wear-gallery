package cc.chenhe.weargallery.ui.local

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.*

class FabBehavior(context: Context?, attrs: AttributeSet?) :
    CoordinatorLayout.Behavior<FloatingActionButton>(context, attrs) {

    /**
     * A coroutine scope use to cancel showing fab button job when the view is destroyed.
     * Button will show immediately when scrolling stops if not provided.
     */
    var scope: CoroutineScope? = null

    private var showJob: Job? = null

    override fun onStartNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: FloatingActionButton,
        directTargetChild: View,
        target: View,
        axes: Int,
        type: Int
    ): Boolean {
        return axes == ViewCompat.SCROLL_AXIS_VERTICAL
    }

    override fun onNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: FloatingActionButton,
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int,
        consumed: IntArray
    ) {
        super.onNestedScroll(
            coordinatorLayout,
            child,
            target,
            dxConsumed,
            dyConsumed,
            dxUnconsumed,
            dyUnconsumed,
            type,
            consumed
        )
        if (dyConsumed > 0 || dyUnconsumed > 0) {
            if (child.isOrWillBeShown) {
                showJob?.cancel()
                child.hide()
            }
        } else {
            if (child.isOrWillBeHidden) {
                showJob?.cancel()
                child.show()
            }
        }
    }

    override fun onStopNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: FloatingActionButton,
        target: View,
        type: Int
    ) {
        super.onStopNestedScroll(coordinatorLayout, child, target, type)
        if (child.isOrWillBeHidden) {
            showJob = scope?.launch(Dispatchers.Main) {
                delay(500L)
                child.show()
            } ?: kotlin.run {
                child.show()
                null
            }
        }
    }
}