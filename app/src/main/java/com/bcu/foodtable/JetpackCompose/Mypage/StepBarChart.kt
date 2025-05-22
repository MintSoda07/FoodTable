package com.bcu.foodtable.JetpackCompose.Mypage.StepBarChart

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.bcu.foodtable.JetpackCompose.Mypage.Health.StepData
import com.bcu.foodtable.R

@Composable
fun StepBarChart(
    stepData: List<StepData>,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            LineChart(context).apply {
                setBackgroundColor(Color.WHITE)
                description.isEnabled = false
                setTouchEnabled(true)
                isDragEnabled = true
                setScaleEnabled(true)
                setPinchZoom(true)
                axisRight.isEnabled = false

                xAxis.apply {
                    textColor = Color.DKGRAY
                    setDrawGridLines(false)
                    granularity = 1f
                    position = XAxis.XAxisPosition.BOTTOM
                    valueFormatter = IndexAxisValueFormatter(stepData.map { it.date })
                    textSize = 14f
                    typeface = Typeface.DEFAULT_BOLD
                    labelRotationAngle = 45f
                }

                axisLeft.apply {
                    textColor = Color.DKGRAY
                    setDrawGridLines(true)
                    gridColor = Color.LTGRAY
                    textSize = 14f
                }

                legend.isEnabled = false

                marker = StepMarkerView(context, R.layout.marker_view)

                // context 저장
                tag = context
            }
        },
        update = { chart ->
            val context = chart.tag as? Context
            val drawable = context?.getDrawable(R.drawable.line_gradient)

            val entries = stepData.mapIndexed { index, data ->
                Entry(index.toFloat(), data.steps.toFloat())
            }

            val dataSet = LineDataSet(entries, "걸음 수").apply {
                color = Color.parseColor("#E76F51")
                setDrawCircles(true)
                circleRadius = 8f
                circleHoleRadius = 4f
                setCircleColor(Color.parseColor("#E76F51"))
                lineWidth = 4f
                mode = LineDataSet.Mode.CUBIC_BEZIER
                setDrawFilled(true)
                fillDrawable = drawable
                fillAlpha = 200
                valueTextColor = Color.DKGRAY
                valueTextSize = 12f
                setDrawValues(true)
                highLightColor = Color.BLACK
            }

            chart.data = LineData(dataSet)
            chart.invalidate()
            chart.animateXY(1500, 1500)
        }
    )
}
