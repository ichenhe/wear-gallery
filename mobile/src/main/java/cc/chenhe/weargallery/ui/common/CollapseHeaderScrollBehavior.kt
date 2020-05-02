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

package cc.chenhe.weargallery.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.children
import cc.chenhe.weargallery.utils.getTitleTextView
import java.lang.ref.WeakReference
import kotlin.math.max
import kotlin.math.min

/**
 * Behavior for collapse header view. This behavior must be used by [CollapseHeaderLayout], if a [Toolbar] is needed
 * it must be the direct child of [CollapseHeaderLayout].
 *
 * The corresponding main content view must have [CollapseContentScrollBehavior].
 *
 * @see [CollapseContentScrollBehavior]
 */
class CollapseHeaderScrollBehavior constructor(context: Context?, attrs: AttributeSet?) :
        CoordinatorLayout.Behavior<CollapseHeaderLayout>(context, attrs) {

    private var toolbarTitleRef: WeakReference<TextView>? = null

    override fun onAttachedToLayoutParams(params: CoordinatorLayout.LayoutParams) {
        super.onAttachedToLayoutParams(params)
        toolbarTitleRef = null
    }

    override fun onDetachedFromLayoutParams() {
        super.onDetachedFromLayoutParams()
        toolbarTitleRef = null
    }

    override fun layoutDependsOn(parent: CoordinatorLayout, child: CollapseHeaderLayout, dependency: View): Boolean {
        return (dependency.layoutParams as CoordinatorLayout.LayoutParams).behavior is CollapseContentScrollBehavior
    }

    override fun onDependentViewChanged(parent: CoordinatorLayout, child: CollapseHeaderLayout, dependency: View): Boolean {
        val progress = -dependency.translationY / (child.height - child.getToolbarHeight())

        child.children.forEach {
            if (it is Toolbar) {
                it.translationY = dependency.translationY
                if (toolbarTitleRef == null) {
                    it.getTitleTextView()?.let { tv -> toolbarTitleRef = WeakReference(tv) }
                }
                toolbarTitleRef?.get()?.alpha = calculateToolBarAlpha(progress)
            } else {
                it.translationY = dependency.translationY * 0.5f
                it.alpha = calculateChildAlpha(progress)
            }
        }
        return true
    }

    /**
     * @param progress [0,1] 0 means header view is fully expended.
     */
    private fun calculateChildAlpha(progress: Float): Float {
        return 1 - min(progress / 0.5f, 1f)
    }

    /**
     * @param progress [0,1] 0 means header view is fully expended.
     */
    private fun calculateToolBarAlpha(progress: Float): Float {
        return max((progress - 0.5f) / 0.5f, 0f)
    }

}