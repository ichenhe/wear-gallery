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

package cc.chenhe.weargallery.common.view

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.SoundEffectConstants
import android.view.View
import android.widget.Checkable
import androidx.core.animation.doOnEnd
import androidx.core.content.res.use
import cc.chenhe.weargallery.common.R
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

private const val ANIM_DURATION = 100L

class CircleCheckBox : View, Checkable {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            .apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT) }
    private lateinit var iconBitmap: Bitmap

    private var circleChecked: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                cancelAllAnimation()
                (if (value) checkAnimatorSet else normalAnimatorSet)?.start() ?: refreshState()
            }
        }

    private var centerX = 0f
    private var centerY = 0f
    private var backgroundRMax = 0f // the maximal radius of background circle
        set(value) {
            field = value
            refreshOutlineR()
        }

    // outline
    private var outlineR = 0f
        set(value) {
            field = value
            animationR = outlineR * 0.8f
        }
    private var outlineMargin = false
        set(value) {
            field = value
            refreshOutlineR()
        }

    // for animation
    private var currentOutlineR = 0f
        set(value) {
            field = value
        }
    private var currentBackgroundR = 0f
        set(value) {
            field = value
            invalidate() // related to animation
        }
    private var currentBackgroundAlpha = 255
        set(value) {
            field = value
        }
    private var iconPercent = 0f
        set(value) {
            field = value
        }
    private var checkAnimatorSet: AnimatorSet? = null
    private var normalAnimatorSet: AnimatorSet? = null

    // icon mask
    private val iconRect = Rect()
    private var animationR = 0f // the radius that animation will scale to

    // attrs
    private var normalColor = 0
    private var checkedColor = 0
    private var outlineColor = 0
    private var outlineWidth = 0f
        set(value) {
            field = value
            refreshOutlineR()
        }

    // -------------------------------------------------------
    // Refresh the relevant properties
    // -------------------------------------------------------

    private fun refreshOutlineR() {
        outlineR = if (outlineMargin) {
            backgroundRMax - outlineWidth / 2f - resources.displayMetrics.density
        } else {
            backgroundRMax - outlineWidth / 2f
        }
    }

    private fun refreshBackgroundRMax() {
        backgroundRMax = min(measuredWidth, measuredHeight) / 2f
    }

    // -------------------------------------------------------
    // Constructors
    // -------------------------------------------------------

    init {
        // prevent xfermod lead to black instead of transparency
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, R.attr.circleCheckBoxStyle)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
            : this(context, attrs, defStyleAttr, R.style.DefaultCircleCheckBoxStyle)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int)
            : super(context, attrs, defStyleAttr, defStyleRes) {
        context.theme.obtainStyledAttributes(attrs, R.styleable.CircleCheckBox, defStyleAttr, defStyleRes).use {
            circleChecked = it.getBoolean(R.styleable.CircleCheckBox_android_checked, circleChecked)
            normalColor = it.getColor(R.styleable.CircleCheckBox_normalColor, normalColor)
            checkedColor = it.getColor(R.styleable.CircleCheckBox_checkedColor, checkedColor)
            outlineColor = it.getColor(R.styleable.CircleCheckBox_outlineColor, outlineColor)
            outlineWidth = it.getDimension(R.styleable.CircleCheckBox_outlineStrokeWidth, 0f)
            outlineMargin = it.getBoolean(R.styleable.CircleCheckBox_outlineMargin, false)
        }
    }


    // -------------------------------------------------------
    // Functions
    // -------------------------------------------------------

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        var viewWidth = resources.getDimensionPixelSize(R.dimen.lib_circlecheckbox_default_size)
        var viewHeight = viewWidth

        viewWidth = when (MeasureSpec.getMode(widthMeasureSpec)) {
            MeasureSpec.EXACTLY -> MeasureSpec.getSize(widthMeasureSpec)
            MeasureSpec.AT_MOST -> min(viewWidth, MeasureSpec.getSize(widthMeasureSpec))
            else -> viewWidth
        }
        viewHeight = when (MeasureSpec.getMode(heightMeasureSpec)) {
            MeasureSpec.EXACTLY -> MeasureSpec.getSize(heightMeasureSpec)
            MeasureSpec.AT_MOST -> min(viewHeight, MeasureSpec.getSize(heightMeasureSpec))
            else -> viewHeight
        }
        setMeasuredDimension(viewWidth, viewHeight)

        refreshBackgroundRMax()
        iconBitmap = createCheckBitmap()
        centerX = viewWidth / 2f
        centerY = viewHeight / 2f
        refreshState()
        initAnimator()
    }

    private fun createCheckBitmap(): Bitmap {
        val drawable = resources.getDrawable(R.drawable.lib_ic_check, null)
        val size = (backgroundRMax / sin(PI / 4.0)).toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    /**
     * Show the latest state without playing animation.
     */
    private fun refreshState() {
        currentOutlineR = if (circleChecked) 0f else outlineR
        currentBackgroundR = if (circleChecked) backgroundRMax else 0f
        currentBackgroundAlpha = 255
        iconPercent = if (circleChecked) 1f else 0f
        invalidate()
    }

    private fun initAnimator() {
        if (checkAnimatorSet == null) {
            checkAnimatorSet = AnimatorSet().apply {
                play(ObjectAnimator.ofFloat(this@CircleCheckBox, "currentOutlineR", outlineR, animationR))
                        .with(ObjectAnimator.ofInt(this@CircleCheckBox, "currentBackgroundAlpha", 0, 255))
                        .with(ObjectAnimator.ofFloat(this@CircleCheckBox, "currentBackgroundR",
                                outlineR + outlineWidth / 2f, animationR + outlineWidth / 2f))
                        .before(ObjectAnimator.ofFloat(this@CircleCheckBox, "currentBackgroundR",
                                animationR + outlineWidth / 2f, backgroundRMax))
                        .before(ObjectAnimator.ofFloat(this@CircleCheckBox, "iconPercent",
                                0f, 1f))
                doOnEnd { currentOutlineR = 0f }
                duration = ANIM_DURATION
            }
        }
        if (normalAnimatorSet == null) {
            normalAnimatorSet = AnimatorSet().apply {
                play(ObjectAnimator.ofFloat(this@CircleCheckBox, "iconPercent", 1f, 0f))
                        .with(ObjectAnimator.ofFloat(this@CircleCheckBox, "currentBackgroundR",
                                backgroundRMax, animationR + outlineWidth / 2f))
                        .before(ObjectAnimator.ofFloat(this@CircleCheckBox, "currentBackgroundR",
                                animationR + outlineWidth / 2f, outlineR + outlineWidth / 2f))
                        .before(ObjectAnimator.ofInt(this@CircleCheckBox, "currentBackgroundAlpha", 255, 0))
                        .before(ObjectAnimator.ofFloat(this@CircleCheckBox, "currentOutlineR", animationR, outlineR))

                duration = ANIM_DURATION
            }
        }
    }

    private fun cancelAllAnimation() {
        checkAnimatorSet?.let {
            if (it.isRunning) {
                it.cancel()
            }
        }
        normalAnimatorSet?.let {
            if (it.isRunning) {
                it.cancel()
            }
        }
    }

    private fun switch2NormalBg() {
        paint.apply {
            style = Paint.Style.FILL
            color = normalColor
        }
    }

    private fun switch2Background() {
        paint.apply {
            style = Paint.Style.FILL
            color = checkedColor
            alpha = currentBackgroundAlpha
        }
    }

    private fun switch2Outline() {
        paint.apply {
            style = Paint.Style.STROKE
            color = outlineColor
            strokeWidth = outlineWidth
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (currentBackgroundR < backgroundRMax) {
            switch2NormalBg()
            canvas.drawCircle(centerX, centerY, backgroundRMax, paint)
        }

        if (currentOutlineR > 0) {
            switch2Outline()
            canvas.drawCircle(centerX, centerY, currentOutlineR, paint)
        }

        if (currentBackgroundR > 0) {
            switch2Background()
            canvas.drawCircle(centerX, centerY, currentBackgroundR, paint)
        }

        if (iconPercent > 0) {
            iconRect.left = ((measuredWidth - iconBitmap.width * iconPercent) / 2f).toInt()
            iconRect.top = ((measuredHeight - iconBitmap.height * iconPercent) / 2f).toInt()
            iconRect.right = measuredWidth - iconRect.left
            iconRect.bottom = measuredHeight - iconRect.top
            canvas.clipRect(iconRect)
            canvas.drawBitmap(iconBitmap, (measuredWidth - iconBitmap.width) / 2f,
                    (measuredHeight - iconBitmap.height) / 2f, checkPaint)
        }
    }

    override fun performClick(): Boolean {
        toggle()
        val handled = super.performClick()
        if (!handled) {
            playSoundEffect(SoundEffectConstants.CLICK)
        }
        return handled
    }

    // ----------------------------------------------------
    // Checkable
    // ----------------------------------------------------

    override fun isChecked(): Boolean = circleChecked

    override fun toggle() {
        circleChecked = !circleChecked
    }

    override fun setChecked(checked: Boolean) {
        circleChecked = checked
    }
}