package com.bcu.foodtable.JetpackCompose.Social.Appointment

import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

private val KST: ZoneId = ZoneId.of("Asia/Seoul")
private val LOCALE_KO: Locale = Locale.KOREAN

/** 예시
 *  - 같은 날: 9월 11일(목) 오전 11:00–오후 1:00
 *  - 다른 날: 9월 11일(목) 오전 11:00 ~ 9월 12일(금) 오후 1:00
 *  - 오늘/내일이면 '오늘', '내일'로 축약
 */
fun formatApptRangeKorean(
    startMillis: Long,
    endMillis: Long,
    nowMillis: Long = System.currentTimeMillis()
): String {
    val start = Instant.ofEpochMilli(startMillis).atZone(KST)
    val end = Instant.ofEpochMilli(endMillis).atZone(KST)
    val now = Instant.ofEpochMilli(nowMillis).atZone(KST)

    val sameDay = start.toLocalDate() == end.toLocalDate()
    val startDate = start.toLocalDate()

    val labelForStartDate = when (startDate) {
        now.toLocalDate() -> "오늘"
        now.toLocalDate().plusDays(1) -> "내일"
        else -> start.format(DateTimeFormatter.ofPattern("M월 d일(E)", LOCALE_KO))
    }

    val timeFmt = DateTimeFormatter.ofPattern("a h:mm", LOCALE_KO)           // 오전/오후 h:mm
    val dateTimeFmt = DateTimeFormatter.ofPattern("yyyy년 M월 d일(E) a h:mm", LOCALE_KO)

    return if (sameDay) {
        // 같은 날이면 날짜는 한 번만, 시간 범위로
        "$labelForStartDate ${start.format(timeFmt)}–${end.format(timeFmt)}"
    } else {
        // 날짜가 다르면 양쪽 모두 날짜+시간
        "${start.format(dateTimeFmt)} ~ ${end.format(dateTimeFmt)}"
    }
}