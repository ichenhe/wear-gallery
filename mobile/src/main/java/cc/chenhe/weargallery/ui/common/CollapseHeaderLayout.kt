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
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.children
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.common.view.NestedScrollConstrainLayout

/**
 * A [NestedScrollConstrainLayout] used to show a collapse header view. [CollapseHeaderScrollBehavior] is attached by
 * default. There can be a direct [Toolbar] and a [TextView] with id [R.id.titleTextView]. When [setTitle] is
 * called both of them will be modified.
 *
 * @see CollapseHeaderScrollBehavior
 */
class CollapseHeaderLayout @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : NestedScrollConstrainLayout(context, attrs, defStyleAttr), CoordinatorLayout.AttachedBehavior {

    private var toolbar: Toolbar? = null
    private var titleTextView: TextView? = null

    override fun getBehavior(): CoordinatorLayout.Behavior<*> {
        return CollapseHeaderScrollBehavior(context, null)
    }

    private fun getToolbar(): Toolbar? {
        if (toolbar == null) {
            children.forEach {
                if (it is Toolbar) {
                    toolbar = it
                }
            }
        }
        return toolbar
    }

    private fun getTitleTextView(): TextView? {
        if (titleTextView == null) {
            titleTextView = findViewById(R.id.titleTextView)
        }
        return titleTextView
    }

    fun getToolbarHeight(): Int {
        return getToolbar()?.measuredHeight ?: 0
    }

    fun setTitle(text: CharSequence) {
        getToolbar()?.title = text
        getTitleTextView()?.text = text
    }

    fun setTitle(@StringRes text: Int) {
        getToolbar()?.setTitle(text)
        getTitleTextView()?.setText(text)
    }

}