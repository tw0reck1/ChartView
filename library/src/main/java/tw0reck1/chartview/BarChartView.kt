/*
 * Copyright 2018 Adrian Tworkowski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tw0reck1.chartview

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.animation.DecelerateInterpolator

open class BarChartView : View {

    private object Border {
        const val NONE = 0
        const val LEFT = 1
        const val TOP = 2
        const val RIGHT = 4
        const val BOTTOM = 8
        const val ALL = 15
    }

    private companion object {

        const val DEFAULT_BORDER_WIDTH = 2f
        const val DEFAULT_BORDER_COLOR = Color.DKGRAY
        const val DEFAULT_BORDERS = Border.LEFT or Border.BOTTOM

        const val DEFAULT_BAR_OUTLINE_WIDTH = 0f
        const val DEFAULT_BAR_OUTLINE_COLOR = Color.LTGRAY
        const val DEFAULT_BAR_CORNER_RADIUS = 0f

        const val DEFAULT_INDICATOR_WIDTH = 1f
        const val DEFAULT_INDICATOR_COLOR = Color.LTGRAY
        const val DEFAULT_INDICATOR_STEP = 25
        val DEFAULT_INDICATOR_LABEL_FORMAT: String? = null

        const val DEFAULT_MAX_VALUE = 100

        const val DEFAULT_ANIMATION_TYPE = AnimationType.ONE_BY_ONE
        const val DEFAULT_ANIMATION_DURATION = 1500

        const val UNSET_SIZE = -1f

        private fun dpToPx(context: Context, dp: Float): Float {
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                    context.resources.displayMetrics)
        }

    }

    private var borderWidth = UNSET_SIZE
    private var borderColor = DEFAULT_BORDER_COLOR
    private var borders = DEFAULT_BORDERS

    private var barOutlineWidth = UNSET_SIZE
    private var barOutlineColor = DEFAULT_BAR_OUTLINE_COLOR
    private var barCornerRadius = UNSET_SIZE

    private var indicatorWidth = UNSET_SIZE
    private var indicatorColor = DEFAULT_INDICATOR_COLOR
    private var indicatorStep = DEFAULT_INDICATOR_STEP
    private var indicatorLabelFormat = DEFAULT_INDICATOR_LABEL_FORMAT

    private var maxValue = DEFAULT_MAX_VALUE

    private var animationType = DEFAULT_ANIMATION_TYPE
    private var animationDuration = DEFAULT_ANIMATION_DURATION

    private var chartData: Array<Int>? = null
    private var chartColors: Array<Int>? = null

    private var chartProgress = 1f

    private val barRect = RectF()
    private val boundingRect = RectF()
    private var labelMargin = 0f
    private var chartBitmap: Bitmap? = null

    private var chartAnimator: ValueAnimator? = null
    private val chartAnimatorListener: Animator.AnimatorListener by lazy {
        createAnimatorListener()
    }

    private val barPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val barOutlinePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initAttributes(context, attrs, 0)
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr) {
        initAttributes(context, attrs, defStyleAttr)
        init()
    }

    private fun initAttributes(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        val array = context.theme.obtainStyledAttributes(attrs,
                R.styleable.BarChartView, defStyleAttr, 0)

        borderWidth = array.getDimension(R.styleable.BarChartView_bcv_border_width,
                dpToPx(context, DEFAULT_BORDER_WIDTH))
        borderColor = array.getColor(R.styleable.BarChartView_bcv_border_color, DEFAULT_BORDER_COLOR)
        borders = array.getInteger(R.styleable.BarChartView_bcv_borders, DEFAULT_BORDERS)

        barOutlineWidth = array.getDimension(R.styleable.BarChartView_bcv_bar_outline_width,
                dpToPx(context, DEFAULT_BAR_OUTLINE_WIDTH))
        barOutlineColor = array.getColor(R.styleable.BarChartView_bcv_bar_outline_color,
                DEFAULT_BAR_OUTLINE_COLOR)
        barCornerRadius = array.getDimension(R.styleable.BarChartView_bcv_bar_corner_radius,
                dpToPx(context, DEFAULT_BAR_CORNER_RADIUS))

        indicatorWidth = array.getDimension(R.styleable.BarChartView_bcv_indicator_width,
                dpToPx(context, DEFAULT_INDICATOR_WIDTH))
        indicatorColor = array.getColor(R.styleable.BarChartView_bcv_indicator_color,
                DEFAULT_INDICATOR_COLOR)
        indicatorStep = array.getInteger(R.styleable.BarChartView_bcv_indicator_step,
                DEFAULT_INDICATOR_STEP)
        indicatorLabelFormat = array.getString(R.styleable.BarChartView_bcv_indicator_label_format)
                ?: DEFAULT_INDICATOR_LABEL_FORMAT

        maxValue = array.getInteger(R.styleable.BarChartView_bcv_max_value, DEFAULT_MAX_VALUE)

        animationType = array.getInteger(R.styleable.BarChartView_bcv_animation_type,
                DEFAULT_ANIMATION_TYPE)
        animationDuration = array.getInteger(R.styleable.BarChartView_bcv_animation_duration,
                DEFAULT_ANIMATION_DURATION)

        array.recycle()
    }

    private fun init() {
        barOutlinePaint.style = Paint.Style.STROKE
        barOutlinePaint.strokeWidth = barOutlineWidth
        barOutlinePaint.color = barOutlineColor

        if (maxValue <= 0) {
            throw IllegalArgumentException("Max value ($maxValue) cannot be lower or equal to 0.")
        }
    }

    fun showChart(data: Array<Int>, colors: Array<Int>, type: Int = animationType,
                  duration: Int = animationDuration) {
        chartData = validateData(data)
        chartColors = colors

        if (chartData!!.size != chartColors!!.size) {
            throw IllegalArgumentException("Chart data and color count must be equal.")
        }

        show(type, duration)
    }

    private fun validateData(data: Array<Int>): Array<Int> {
        for (i in data) {
            if (i < 0 || i > maxValue) {
                throw IllegalArgumentException("Chart value ($i) is not in the supported range: [0, $maxValue].")
            }
        }

        return data
    }

    private fun show(type: Int, duration: Int) {
        animationType = type
        animationDuration = duration

        if (type == AnimationType.NONE) {
            chartAnimator?.cancel()
            chartAnimator = null

            chartProgress = 1f
            invalidate()
        } else {
            chartAnimator?.cancel()
            chartAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                this.duration = duration.toLong()
                this.interpolator = DecelerateInterpolator()

                addUpdateListener { animation ->
                    chartProgress = animation.animatedValue as Float

                    invalidate()
                }
                addListener(chartAnimatorListener)

                start()
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        chartBitmap = createChartBitmap(w, h)
        adjustBoundingRect(boundingRect)
    }

    private fun createChartBitmap(width: Int, height: Int): Bitmap? {
        if ((indicatorWidth <= 0f || indicatorStep <= 0) && borderWidth <= 0f) return null

        val bitmapWidth =  width - paddingRight - paddingLeft
        val bitmapHeight =  height - paddingBottom - paddingTop

        val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        linePaint.style = Paint.Style.STROKE

        if (indicatorWidth > 0f && indicatorStep > 0) {
            linePaint.strokeCap = Paint.Cap.BUTT
            linePaint.strokeWidth = indicatorWidth
            linePaint.color = indicatorColor

            val lines = (maxValue / indicatorStep.toFloat()).toInt()
            val startHeight = bitmapHeight.toFloat()
            val spacing = bitmapHeight.toFloat() / (maxValue.toFloat() / indicatorStep)

            var lineHeight: Float

            if (indicatorLabelFormat != null) {
                val longestText = String.format(indicatorLabelFormat!!, maxValue)
                val textBounds = Rect()

                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                textPaint.textSize = indicatorWidth * 12f
                textPaint.color = borderColor
                textPaint.textAlign = Paint.Align.RIGHT
                textPaint.getTextBounds(longestText, 0, longestText.length, textBounds)

                val textWidth = textBounds.width().toFloat()
                val margin = textBounds.height() / 4f

                labelMargin = textWidth + margin

                val textMinY = textBounds.height().toFloat()
                val textMaxY = bitmapHeight.toFloat()
                var textY: Float

                for (i in 0..lines) {
                    lineHeight = startHeight - i * spacing

                    textY = Math.min(textMaxY, Math.max(lineHeight - textBounds.exactCenterY(),
                            textMinY))

                    canvas.drawText(String.format(indicatorLabelFormat!!, indicatorStep * i),
                            textWidth, textY, textPaint)

                    if (i == 0) continue

                    canvas.drawLine(labelMargin, lineHeight, bitmapWidth.toFloat(),
                            lineHeight, linePaint)
                }
            } else {
                for (i in 1..lines) {
                    lineHeight = startHeight - i * spacing

                    canvas.drawLine(0f, lineHeight, bitmapWidth.toFloat(),
                            lineHeight, linePaint)
                }
            }
        }

        if (borderWidth > 0f) {
            linePaint.strokeCap = Paint.Cap.SQUARE
            linePaint.strokeWidth = borderWidth
            linePaint.color = borderColor

            val left = labelMargin + borderWidth / 2f
            val top = borderWidth / 2f
            val right = bitmapWidth - borderWidth / 2f
            val bottom = bitmapHeight - borderWidth / 2f

            if (borders and Border.LEFT == Border.LEFT) {
                canvas.drawLine(left, top, left, bottom, linePaint)
            }

            if (borders and Border.TOP == Border.TOP) {
                canvas.drawLine(left, top, right, top, linePaint)
            }

            if (borders and Border.RIGHT == Border.RIGHT) {
                canvas.drawLine(right, top, right, bottom, linePaint)
            }

            if (borders and Border.BOTTOM == Border.BOTTOM) {
                canvas.drawLine(left, bottom, right, bottom, linePaint)
            }
        }

        return bitmap
    }

    private fun adjustBoundingRect(rectF: RectF) {
        var left = paddingLeft.toFloat() + labelMargin
        var top = paddingTop.toFloat()
        var right = (width - paddingRight).toFloat()
        var bottom = (height - paddingBottom).toFloat()

        if (borderWidth > 0f) {
            if (borders and Border.LEFT == Border.LEFT) left += borderWidth
            if (borders and Border.TOP == Border.TOP) top += borderWidth
            if (borders and Border.RIGHT == Border.RIGHT) right -= borderWidth
            if (borders and Border.BOTTOM == Border.BOTTOM) bottom -= borderWidth
        }

        if (barOutlineWidth > 0f) {
            left += barOutlineWidth / 2f
            top += barOutlineWidth / 2f
            right -= barOutlineWidth / 2f
            bottom -= barOutlineWidth / 2f
        }

        rectF.set(left, top, right, bottom)
    }

    override fun onDraw(canvas: Canvas?) {
        if (canvas == null || chartData == null || chartColors == null) return

        chartBitmap?.let {
            canvas.drawBitmap(it, paddingLeft.toFloat(), paddingTop.toFloat(), null)
        }

        val count = chartData!!.size

        val columnWidth = boundingRect.width() / (2f * count + 1f)
        var columnHeight: Float
        val maxColumnHeight = boundingRect.height()

        var left = boundingRect.left + columnWidth

        if (animationType == AnimationType.NONE) {
            for (a in 0..count - 1) {
                barPaint.color = chartColors!![a]

                columnHeight = maxColumnHeight * (chartData!![a] / maxValue.toFloat())
                barRect.set(left, boundingRect.bottom - columnHeight,
                        left + columnWidth, boundingRect.bottom)

                canvas.drawRoundRect(barRect, barCornerRadius, barCornerRadius, barPaint)
                if (barOutlineWidth > 0f) {
                    canvas.drawRoundRect(barRect, barCornerRadius, barCornerRadius, barOutlinePaint)
                }

                left += 2f * columnWidth
            }
        } else if (animationType == AnimationType.ALL_AT_ONCE) {
            for (a in 0..count - 1) {
                barPaint.color = chartColors!![a]

                columnHeight = maxColumnHeight * (chartData!![a] / maxValue.toFloat()) * chartProgress
                barRect.set(left, boundingRect.bottom - columnHeight,
                        left + columnWidth, boundingRect.bottom)

                canvas.drawRoundRect(barRect, barCornerRadius, barCornerRadius, barPaint)
                if (barOutlineWidth > 0f) {
                    canvas.drawRoundRect(barRect, barCornerRadius, barCornerRadius, barOutlinePaint)
                }

                left += 2f * columnWidth
            }
        } else if (animationType == AnimationType.ONE_BY_ONE) {
            val barProgress = 1f / count

            for (a in 0..count - 1) {
                if (chartProgress < barProgress * (a)) break

                barPaint.color = chartColors!![a]

                val progress = if (chartProgress > barProgress * (a + 1)) {
                    1f
                } else {
                    (chartProgress - barProgress * a) / barProgress
                }

                columnHeight = maxColumnHeight * (chartData!![a] / maxValue.toFloat()) * progress
                barRect.set(left, boundingRect.bottom - columnHeight,left + columnWidth, boundingRect.bottom)

                canvas.drawRoundRect(barRect, barCornerRadius, barCornerRadius, barPaint)
                if (barOutlineWidth > 0f) {
                    canvas.drawRoundRect(barRect, barCornerRadius, barCornerRadius, barOutlinePaint)
                }

                left += 2f * columnWidth
            }
        }
    }

    private fun createAnimatorListener() = object: Animator.AnimatorListener {

        override fun onAnimationStart(animation: Animator?) {}

        override fun onAnimationRepeat(animation: Animator?) {}

        override fun onAnimationCancel(animation: Animator?) {
            chartAnimator = null
        }

        override fun onAnimationEnd(animation: Animator?) {
            chartAnimator = null
        }

    }

}