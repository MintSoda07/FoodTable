package com.bcu.foodtable.JetpackCompose.Mypage

import android.graphics.Color as AndroidColor
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.bcu.foodtable.JetpackCompose.Mypage.StepBarChart.StepMarkerView
import com.bcu.foodtable.R
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

@Composable
fun WeeklyStepChart(
    weeklySteps: List<Int>,  // 7일치 걸음 수 [월, 화, ..., 일]
    modifier: Modifier = Modifier
) {
    val days = listOf("월", "화", "수", "목", "금", "토", "일")

    AndroidView(
        modifier = modifier,
        factory = { context ->
            LineChart(context).apply {
                setBackgroundColor(AndroidColor.WHITE)
                description.isEnabled = false
                setTouchEnabled(true)
                isDragEnabled = false
                setScaleEnabled(false)
                setPinchZoom(false)
                axisRight.isEnabled = false

                xAxis.apply {
                    textColor = AndroidColor.DKGRAY
                    setDrawGridLines(false)
                    granularity = 1f
                    position = XAxis.XAxisPosition.BOTTOM
                    valueFormatter = IndexAxisValueFormatter(days)
                    textSize = 15f
                    typeface = Typeface.DEFAULT_BOLD
                    labelRotationAngle = 45f
                    setAvoidFirstLastClipping(true)
                }

                axisLeft.apply {
                    textColor = AndroidColor.DKGRAY
                    setDrawGridLines(true)
                    gridColor = AndroidColor.LTGRAY
                    textSize = 15f
                    axisMinimum = 0f
                    axisMaximum = (weeklySteps.maxOrNull() ?: 0).toFloat() * 1.2f
                }

                legend.isEnabled = false

                marker = StepMarkerView(context, R.layout.marker_view)
            }
        },
        update = { chart ->
            val entries = weeklySteps.mapIndexed { index, steps ->
                Entry(index.toFloat(), steps.toFloat())
            }

            val dataSet = LineDataSet(entries, "주간 걸음 수").apply {
                color = AndroidColor.parseColor("#FF6F3E")
                lineWidth = 5f
                mode = LineDataSet.Mode.CUBIC_BEZIER
                setDrawCircles(true)
                circleRadius = 12f
                circleHoleRadius = 6f
                setCircleColor(AndroidColor.parseColor("#FF9B66"))
                setDrawFilled(true)
                fillDrawable = chart.context.getDrawable(R.drawable.line_gradient)
                fillAlpha = 220
                valueTextColor = AndroidColor.parseColor("#333333")
                valueTextSize = 16f
                setDrawValues(true)
                highLightColor = AndroidColor.parseColor("#FF5722")
            }

            chart.data = LineData(dataSet)
            chart.invalidate()
            chart.animateXY(1500, 1500)
        }
    )
}
