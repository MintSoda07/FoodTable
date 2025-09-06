package com.bcu.foodtable.ui.merchant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WeekdayMultiSelector(
    selected: Set<String>,
    onChange: (Set<String>) -> Unit
) {
    val options = listOf("MON","TUE","WED","THU","FRI","SAT","SUN")
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 4
    ) {
        options.forEach { d ->
            val isSel = d in selected
            FilterChip(
                selected = isSel,
                onClick = {
                    onChange(if (isSel) selected - d else selected + d)
                },
                label = { Text(dayLabel(d)) },
                leadingIcon = { if (isSel) Icon(Icons.Default.Check, null) else null }
            )
        }
    }
}

/** 요일 약어 → 한국어 풀네임 */
private fun dayLabel(abbrev: String): String = when (abbrev) {
    "MON" -> "월요일"
    "TUE" -> "화요일"
    "WED" -> "수요일"
    "THU" -> "목요일"
    "FRI" -> "금요일"
    "SAT" -> "토요일"
    "SUN" -> "일요일"
    else -> abbrev
}
