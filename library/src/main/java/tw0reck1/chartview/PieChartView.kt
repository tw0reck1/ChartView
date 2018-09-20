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

open class PieChartView : View {

    private companion object {

        const val DEFAULT_BORDER_WIDTH = 1f
        const val DEFAULT_BORDER_COLOR = Color.BLACK

        const val DEFAULT_DONUT_RATIO = 0f

        const val DEFAULT_ANIMATION_TYPE = AnimationType.ALL_AT_ONCE
        const val DEFAULT_ANIMATION_DURATION = 1000

        const val UNSET_SIZE = -1f

        private fun dpToPx(context: Context, dp: Float): Float {
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                    context.resources.displayMetrics)
        }

    }

    private var borderWidth = UNSET_SIZE
    private var borderColor = DEFAULT_BORDER_COLOR

    private var donutRatio = DEFAULT_DONUT_RATIO

    private var animationType = DEFAULT_ANIMATION_TYPE
    private var animationDuration = DEFAULT_ANIMATION_DURATION

    private var chartData: Array<Float>? = null
    private var chartColors: Array<Int>? = null

    private var chartAngle = 360f
    private val chartRect = RectF()
    private var donutRect: RectF? = null
    private var donutPorterMode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)

    private var chartAnimator: ValueAnimator? = null
    private val chartAnimatorListener: Animator.AnimatorListener by lazy {
        createAnimatorListener()
    }

    private val chartPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

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
                R.styleable.PieChartView, defStyleAttr, 0)

        borderWidth = array.getDimension(R.styleable.PieChartView_pcv_border_width,
                dpToPx(context, DEFAULT_BORDER_WIDTH))
        borderColor = array.getColor(R.styleable.PieChartView_pcv_border_color, DEFAULT_BORDER_COLOR)

        donutRatio = array.getFloat(R.styleable.PieChartView_pcv_donut_ratio, DEFAULT_DONUT_RATIO)

        animationType = array.getInteger(R.styleable.PieChartView_pcv_animation_type,
                DEFAULT_ANIMATION_TYPE)
        animationDuration = array.getInteger(R.styleable.PieChartView_pcv_animation_duration,
                DEFAULT_ANIMATION_DURATION)

        array.recycle()
    }

    private fun init() {
        setLayerType(LAYER_TYPE_HARDWARE, chartPaint)

        borderPaint.style = Paint.Style.STROKE
        borderPaint.strokeCap = Paint.Cap.ROUND
        borderPaint.strokeJoin = Paint.Join.ROUND
        borderPaint.strokeWidth = borderWidth
        borderPaint.color = borderColor

        if (donutRatio < 0f || donutRatio >= 1f) {
            throw IllegalArgumentException("Donut ratio ($donutRatio) is not in the supported range: [0, 1).")
        }
    }

    fun showChart(data: Array<Float>, colors: Array<Int>, type: Int = animationType,
                  duration: Int = animationDuration) {
        chartData = validateData(data)
        chartColors = colors

        if (chartData!!.size != chartColors!!.size) {
            throw IllegalArgumentException("Chart data and color count must be equal.")
        }

        show(type, duration)
    }

    private fun validateData(data: Array<Float>): Array<Float> {
        var sum = 0f

        for (f in data) {
            sum += f
        }

        if (sum != 1f) throw IllegalArgumentException("Chart data must sum up to 1.")

        return data
    }

    private fun show(type: Int, duration: Int) {
        animationType = type
        animationDuration = duration

        if (type == AnimationType.NONE) {
            chartAnimator?.cancel()
            chartAnimator = null

            chartAngle = 360f
            invalidate()
        } else {
            chartAnimator?.cancel()
            chartAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
                this.duration = duration.toLong()
                this.interpolator = DecelerateInterpolator()

                addUpdateListener { animation ->
                    chartAngle = animation.animatedValue as Float

                    invalidate()
                }
                addListener(chartAnimatorListener)

                start()
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        val width =  w - paddingRight - paddingLeft
        val height =  h - paddingBottom - paddingTop

        val centerX = paddingLeft + width / 2f
        val centerY = paddingTop + height / 2f

        var radius = Math.min(width, height) / 2f

        var left = centerX - radius
        var right = centerX + radius
        var top = centerY - radius
        var bottom = centerY + radius

        chartRect.set(left, top, right, bottom)

        if (donutRatio > 0f) {
            radius *= donutRatio

            left = centerX - radius
            right = centerX + radius
            top = centerY - radius
            bottom = centerY + radius

            donutRect = RectF(left, top, right, bottom)
        }
    }

    override fun onDraw(canvas: Canvas?) {
        if (canvas == null || chartData == null || chartColors == null) return

        var angle = -90f
        var sweep: Float

        val count = chartData!!.size

        if (animationType == AnimationType.NONE) {
            for (a in 0..count - 1) {
                sweep = chartData!![a] * 360f
                chartPaint.color = chartColors!![a]
                canvas.drawArc(chartRect,
                        angle, sweep, true, chartPaint)

                if (borderWidth > 0f) {
                    canvas.drawArc(chartRect,
                            angle, sweep, true, borderPaint)
                }

                angle += sweep
            }
        } else if (animationType == AnimationType.ALL_AT_ONCE) {
            for (a in 0..count - 1) {
                sweep = chartData!![a] * chartAngle
                chartPaint.color = chartColors!![a]
                canvas.drawArc(chartRect,
                        angle, sweep, true, chartPaint)

                if (borderWidth > 0f) {
                    canvas.drawArc(chartRect,
                            angle, sweep, true, borderPaint)
                }

                angle += sweep
            }
        } else if (animationType == AnimationType.ONE_BY_ONE) {
            var availableSweep = chartAngle
            for (a in 0..count - 1) {
                if (availableSweep <= 0f) break

                sweep = Math.min(chartData!![a] * 360f, availableSweep)

                chartPaint.color = chartColors!![a]
                canvas.drawArc(chartRect,
                        angle, sweep, true, chartPaint)

                if (borderWidth > 0f) {
                    canvas.drawArc(chartRect,
                            angle, sweep, true, borderPaint)
                }

                angle += sweep
                availableSweep -= sweep
            }
        }

        donutRect?.let {
            val donutSize = if (borderWidth > 0f) {
                canvas.drawArc(it, -90f, angle + 90f, true, borderPaint)

                it.width() / 2f - borderWidth / 2f
            } else {
                it.width() / 2f
            }

            chartPaint.xfermode = donutPorterMode
            canvas.drawCircle(it.centerX(), it.centerY(), donutSize, chartPaint)
            chartPaint.xfermode = null
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