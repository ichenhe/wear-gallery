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

class DragSelectProcessor(
        private val mode: Mode,
        private val selectionHandler: SelectionHandler
) : DragSelectTouchListener.OnDragSelectListener {

    enum class Mode {
        Simple
    }

    override fun onSelectionStarted(position: Int) {
        selectionHandler.onSelectionStarted(position)
        when (mode) {
            Mode.Simple -> selectionHandler.updateSelection(position, position, true, true)
        }
    }

    override fun onSelectionFinished(position: Int) {
        selectionHandler.onSelectionFinished(position)
    }

    override fun onSelectChange(start: Int, end: Int, isSelected: Boolean) {
        when (mode) {
            Mode.Simple -> {
                selectionHandler.updateSelection(start, end, isSelected, false)
            }
        }
    }

    interface SelectionHandler {

        fun updateSelection(start: Int, end: Int, isSelected: Boolean, calledFromOnStart: Boolean)

        fun onSelectionStarted(position: Int)

        fun onSelectionFinished(position: Int)
    }

    open class SimpleSelectionHandler : SelectionHandler {

        override fun updateSelection(start: Int, end: Int, isSelected: Boolean, calledFromOnStart: Boolean) {
        }

        override fun onSelectionStarted(position: Int) {
        }

        override fun onSelectionFinished(position: Int) {
        }
    }

}