package ru.netology.statsview.ui

import android.animation.*
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.animation.doOnStart
import androidx.core.content.withStyledAttributes
import ru.netology.statsview.R
import ru.netology.statsview.utils.AndroidUtils
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class StatsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr, defStyleRes) {
    private var radius = 0F
    private var center = PointF(0F, 0F)
    private var oval = RectF(0F, 0F, 0F, 0F)
    private var fontSize = AndroidUtils.dp(context, 20F).toFloat()
    private var lineWidth = AndroidUtils.dp(context, 5F).toFloat()
    private var colors = emptyList<Int>()
    private var progress = 0F
    private var rotationAngle = 0F
    private var valueAnimator: ValueAnimator? = null
    private var fillMode = 0
    private var curveProgress = mutableListOf<Float>()

    init {
        context.withStyledAttributes(attrs, R.styleable.StatsView) {
            fontSize = getDimension(R.styleable.StatsView_fontSize, fontSize)
            lineWidth = getDimension(R.styleable.StatsView_lineWidth, lineWidth)
            fillMode = getInt(R.styleable.StatsView_fillMode, 0)

            val resId = getResourceId(R.styleable.StatsView_colors, 0)
            colors = resources.getIntArray(resId).toList()
        }
    }

    private val paint = Paint(
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = lineWidth
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }
    )
    private val textPaint = Paint(
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = fontSize
            style = Paint.Style.FILL
            textAlign = Paint.Align.CENTER
        }
    )

    var data: List<Float> = emptyList()
        set(value) {
            field = value
            update()
        }

    private val dotPaint = Paint(
        Paint.ANTI_ALIAS_FLAG
    ).apply {
        color = colors[0]
        strokeWidth = lineWidth
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private val backPaint = Paint(
        Paint.ANTI_ALIAS_FLAG
    ).apply {
        strokeWidth = lineWidth
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        radius = min(w, h) / 2F - lineWidth
        center = PointF(w / 2F, h / 2F)
        oval = RectF(
            center.x - radius, center.y - radius,
            center.x + radius, center.y + radius
        )
    }

    override fun onDraw(canvas: Canvas) {
        backPaint.color = Color.GRAY
        backPaint.alpha = 10
        canvas.save()
        canvas.rotate(rotationAngle, center.x, center.y)
        canvas.drawCircle(center.x, center.y, radius, backPaint)

        if (data.isEmpty()) {
            canvas.restore()
            return
        }

        var startAngle = -90F
        var sumPercent = max(1F, data.sum())
        sumPercent = if (sumPercent < 1) 1F else sumPercent

        for ((index, datum) in data.withIndex()) {
            val angle = 360F * datum
            paint.color = colors.getOrNull(index) ?: randomColor()
            val sweepAngle = angle * curveProgress.getOrElse(index) { 0F }
            canvas.drawArc(oval, startAngle, sweepAngle, false, paint)
            startAngle += angle
        }
        canvas.drawPoint(center.x, center.y - radius, dotPaint)
        canvas.restore()

        canvas.drawText(
            "%.2f%%".format(data.sum() * 100 / sumPercent),
            center.x,
            center.y + textPaint.textSize / 4,
            textPaint
        )

    }

    private fun update() {
        valueAnimator?.let {
            it.removeAllListeners()
            it.cancel()
        }
        progress = 0F
        rotationAngle = 0F
        curveProgress = MutableList(data.size) { 0F }

        if (fillMode == 0) {
            parallelFill()
        } else {
            animateConsistent()
        }
    }

    private fun parallelFill() { // Параллельное заполнение
        val animators = mutableListOf<Animator>()

        for (index in data.indices) {
            val animator = ValueAnimator.ofFloat(0F, 1F).apply {
                duration = 2000
                interpolator = LinearInterpolator()
                addUpdateListener { anim ->
                    curveProgress[index] = anim.animatedValue as Float
                    invalidate()
                }
            }
            animators.add(animator)
        }

        val rotationAnimator = ValueAnimator.ofFloat(0F, 360F).apply {
            duration = 2000
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                rotationAngle = anim.animatedValue as Float
                invalidate()
            }
        }

        AnimatorSet().apply {
            playTogether(animators + rotationAnimator)
            start()
        }
    }

    private fun animateConsistent() { // Последовательное заполнение
        val animators = mutableListOf<Animator>()

        for ((index, datum) in data.withIndex()) {
            val animator = ValueAnimator.ofFloat(0F, 1F).apply {
                duration = (500 + (1000 * datum)).toLong()
                interpolator = LinearInterpolator()
                addUpdateListener { anim ->
                    curveProgress[index] = anim.animatedValue as Float
                    invalidate()
                }
            }
            animators.add(animator)
        }

        AnimatorSet().apply {
            playSequentially(animators)
            start()
        }
    }

    private fun randomColor() = Random.nextInt(0xFF000000.toInt(), 0xFFFFFFFF.toInt())
}