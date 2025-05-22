package com.bcu.foodtable.ui.myPage

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
        val centerY = height / 2  //
        val offsetY = 0f          //

        val rect = RectF(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )

        // 반원만 위쪽만 나오도록 180도
        canvas.drawArc(rect, 180f, 180f, false, bgArcPaint)
        canvas.drawArc(rect, 180f, 180f * progress, false, arcPaint)

        // 텍스트 위치도 기준 변경
        canvas.drawText("오늘의 걸음 수", centerX, centerY - 40f, labelTextPaint)
        canvas.drawText(stepText, centerX, centerY + 20f, stepTextPaint)
        canvas.drawText(goalText, centerX, centerY + 80f, labelTextPaint)
    }


    fun setStepData(currentSteps: Int, goalSteps: Int) {
        progress = currentSteps.coerceAtMost(goalSteps).toFloat() / goalSteps
        stepText = "$currentSteps 걸음"
        goalText = "목표 $goalSteps"
    }
}
