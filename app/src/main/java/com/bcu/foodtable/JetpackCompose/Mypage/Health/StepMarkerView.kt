package com.bcu.foodtable.JetpackCompose.Mypage.Health

import android.content.Context
import android.widget.TextView
import com.bcu.foodtable.R
import com.bcu.foodtable.JetpackCompose.Mypage.Health.StepData
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class StepMarkerView(
    context: Context,
    layoutResource: Int,
    private val stepDataList: List<StepData> // ← X축 데이터 전달받음
) : MarkerView(context, layoutResource) {

    private val tvContent: TextView = findViewById(R.id.tvContent)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        val index = e?.x?.toInt() ?: return
        if (index in stepDataList.indices) {
            val rawDate = stepDataList[index].date // 예: "20240527" 또는 "05.27 (월)"
            val steps = e.y.toInt()

            // 날짜 포맷 시도
            val formattedDate = try {
                val parsed = LocalDate.parse(rawDate, DateTimeFormatter.ofPattern("yyyyMMdd"))
                parsed.format(DateTimeFormatter.ofPattern("MM.dd (E)", Locale.KOREAN)) // "05.27 (월)"
            } catch (e: Exception) {
                rawDate // 실패 시 원본 출력
            }

            tvContent.text = "$formattedDate\n걸음 수: $steps"
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2).toFloat(), -height.toFloat())
    }
}
