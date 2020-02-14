/*
 * Copyright (c) 2020 Chenhe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cc.chenhe.weargallery.wearvision.widget

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import cc.chenhe.weargallery.wearvision.util.isScreenRound
import kotlin.math.max
import kotlin.math.min

/**
 * A layout that extends [CoordinatorLayout].
 *
 * This layout adds vertical edge scrolling effects that adapts the round screen.
 */
class VisionCoordinatorLayout @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : CoordinatorLayout(context, attrs, defStyleAttr) {

    private var mEdgeGlowTop: EdgeEffect? = null
    private var mEdgeGlowBottom: EdgeEffect? = null

    init {
        if (!isInEditMode) {
            updateSystemShape(isScreenRound(context))
        }
        setWillNotDraw(false)
    }

    private fun updateSystemShape(systemIsRound: Boolean) {
        if (systemIsRound) {
            if (mEdgeGlowTop == null || mEdgeGlowTop !is CrescentEdgeEffect) {
                mEdgeGlowTop = CrescentEdgeEffect(context)
                mEdgeGlowBottom = CrescentEdgeEffect(context)
            }
        } else {
            if (mEdgeGlowTop == null || mEdgeGlowTop !is ClassicEdgeEffect) {
                mEdgeGlowTop = ClassicEdgeEffect(context)
                mEdgeGlowBottom = ClassicEdgeEffect(context)
            }
        }
//        mScrollBarHelper.setIsRound(systemIsRound)
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        mEdgeGlowTop?.let { top ->
            if (!top.isFinished) {
                val restoreCount = canvas.save()
                val width = width
                val edgeY = min(0, scrollY)
                canvas.translate(0f, edgeY.toFloat())
                top.setSize(width, height)
                if (top.draw(canvas)) {
                    postInvalidateOnAnimation(0, 0, getWidth(), top.maxHeight + paddingTop)
                }
                canvas.restoreToCount(restoreCount)
            }
        }

        mEdgeGlowBottom?.let { bottom ->
            if (!bottom.isFinished) {
                val restoreCount = canvas.save()
                val width = width
                val height = height
                val edgeX = -width
                val edgeY = max(height, scrollY + height)
                canvas.translate(edgeX.toFloat(), edgeY.toFloat())
                canvas.rotate(180f, width.toFloat(), 0f)
                bottom.setSize(width, height)
                if (bottom.draw(canvas)) {
                    postInvalidateOnAnimation(0, getHeight() - paddingBottom - bottom.maxHeight,
                            getWidth(), getHeight())
                }
                canvas.restoreToCount(restoreCount)
            }
        }

    }

    override fun onStartNestedScroll(child: View, target: View, axes: Int, type: Int): Boolean {
        super.onStartNestedScroll(child, target, axes, type)
        return true // Always accept the nested scroll to show over scroll effects.
    }

    override fun onNestedScroll(target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int,
                                type: Int, consumed: IntArray) {
        super.onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type, consumed)
        pullEdgeEffects(dyUnconsumed)
    }

    override fun onNestedFling(target: View, velocityX: Float, velocityY: Float, consumed: Boolean): Boolean {
        if (!consumed) {
            absorbEdgeEffects(velocityY.toInt())
        }
        return true
    }

    override fun onStopNestedScroll(target: View, type: Int) {
        super.onStopNestedScroll(target, type)
        if (type == ViewCompat.TYPE_TOUCH) {
            releaseEdgeEffects()
        }
    }

    private fun pullEdgeEffects(dyUnconsumed: Int) {
        val maxPullEffectDistance = height / 2
        if (dyUnconsumed < 0) {
            mEdgeGlowTop?.let { top ->
                top.onPull(-dyUnconsumed.toFloat() / maxPullEffectDistance)
                if (mEdgeGlowBottom?.isFinished == false) {
                    mEdgeGlowBottom?.onRelease()
                }
                invalidate()
            }

        } else if (dyUnconsumed > 0) {
            mEdgeGlowBottom?.let { bottom ->
                bottom.onPull(dyUnconsumed.toFloat() / maxPullEffectDistance)
                if (mEdgeGlowTop?.isFinished == false) {
                    mEdgeGlowTop?.onRelease()
                }
                invalidate()
            }
        }
    }

    private fun absorbEdgeEffects(velY: Int) {
        if (velY > 0) {
            mEdgeGlowBottom?.onAbsorb(velY)
        } else if (velY < 0) {
            mEdgeGlowTop?.onAbsorb(velY)
        }
    }

    private fun releaseEdgeEffects() {
        mEdgeGlowTop?.onRelease()
        mEdgeGlowBottom?.onRelease()
    }

}