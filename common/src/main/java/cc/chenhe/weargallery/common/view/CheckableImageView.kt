package cc.chenhe.weargallery.common.view

import android.content.Context
import android.util.AttributeSet
import android.widget.Checkable
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.res.use
import cc.chenhe.weargallery.common.R

class CheckableImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr), Checkable {

    private val checkedStateSet = intArrayOf(android.R.attr.state_checked)

    private var _checked = false
        set(value) {
            if (value != field) {
                field = value
                refreshDrawableState()
            }
        }

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.CheckableImageView, defStyleAttr, 0)
            .use {
                _checked = it.getBoolean(R.styleable.CheckableImageView_android_checked, false)
            }
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val drawableState = super.onCreateDrawableState(extraSpace + 1)
        if (_checked) {
            mergeDrawableStates(drawableState, checkedStateSet)
        }
        return drawableState
    }

    // --------------------------------------------
    // Checkable
    // --------------------------------------------

    override fun isChecked(): Boolean = _checked

    override fun toggle() {
        _checked = !_checked
    }

    override fun setChecked(checked: Boolean) {
        _checked = checked
    }
}