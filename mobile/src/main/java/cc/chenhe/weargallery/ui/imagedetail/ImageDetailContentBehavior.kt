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

package cc.chenhe.weargallery.ui.imagedetail

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import cc.chenhe.weargallery.R

/**
 * The [CoordinatorLayout.Behavior] for image detail content ([androidx.viewpager2.widget.ViewPager2]).
 * This behavior fully depends on the [R.id.imageDetailPanel] and doesn't save state when rotate.
 */
class ImageDetailContentBehavior(context: Context?, attrs: AttributeSet?)
    : CoordinatorLayout.Behavior<View>(context, attrs) {

    private var parentHeight = 0
    private var top = Int.MIN_VALUE

    override fun onLayoutChild(parent: CoordinatorLayout, child: View, layoutDirection: Int): Boolean {
        parent.onLayoutChild(child, layoutDirection)
        if (top != Int.MIN_VALUE) {
            child.top = top
        }
        return true
    }

    override fun layoutDependsOn(parent: CoordinatorLayout, child: View, dependency: View): Boolean {
        return dependency.id == R.id.imageDetailPanel
    }

    override fun onDependentViewChanged(parent: CoordinatorLayout, child: View, dependency: View): Boolean {
        if (parentHeight == 0) {
            parentHeight = parent.height
        }
        top = (dependency.top - parent.height) / 2
        child.top = top
        return true
    }
}