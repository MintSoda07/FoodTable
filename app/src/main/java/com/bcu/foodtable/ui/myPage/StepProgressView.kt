package com.bcu.foodtable.ui.health

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class StepProgressView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var progress: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    var stepText: String = "0 걸음"
        set(value) {
            field = value
            invalidate()
        }

    var goalText: String = "목표 5000"
        set(value) {
            field = value
            invalidate()
        }

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#990000")
        strokeWidth = 30f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val bgArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        strokeWidth = 30f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val stepTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 64f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private val labelTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        textSize = 40f
        textAlign = Paint.Align.CENTER
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        val radius = width / 2.5f
        val centerX = width / 2
        val centerY = height
        val offsetY = 60f // 아래로 내리는 정도
        val rect = RectF(centerX - radius, centerY - radius * 2 - offsetY, centerX + radius, centerY)

        // 배경 반원
        canvas.drawArc(rect, 180f, 180f, false, bgArcPaint)
        // 진행 반원
        canvas.drawArc(rect, 180f, 180f * progress, false, arcPaint)

        // 텍스트
        canvas.drawText("오늘의 걸음 수", centerX, height / 2.6f, labelTextPaint)
        canvas.drawText(stepText, centerX, height / 2.2f, stepTextPaint)
        canvas.drawText(goalText, centerX, height / 1.8f, labelTextPaint)
    }

    fun setStepData(currentSteps: Int, goalSteps: Int) {
        progress = currentSteps.coerceAtMost(goalSteps).toFloat() / goalSteps
        stepText = "$currentSteps 걸음"
        goalText = "목표 $goalSteps"
    }
}
