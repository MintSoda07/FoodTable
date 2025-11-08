package com.bcu.foodtable.ui.merchant

import com.google.firebase.Timestamp

/** 영업시간: 초기에는 빈 문자열(미설정)로 시작 */
data class BusinessHours(
    val open: String = "",
    val close: String = "",
    val closed: Boolean = false
)

/** 더미 기본값 제거: 가능한 한 빈/널 상태로 시작 */
data class StoreProfile(
    val storeId: String = "",

    // 기본 정보
    val storeName: String = "",
    val category: String = "",
    val description: String = "",

    // 연락/정책
    val phone: String = "",
    val address: String = "",
    val minOrderPrice: Long? = null,   // 미설정 null
    val taxPercent: Double? = null,    // 미설정 null

    // 상태/설정 (미설정 null → UI에서 ?: false 로 표시만 기본)
    val status: String = "",
    val onlineOrderEnabled: Boolean? = null,
    val openNow: Boolean? = null,
    val takeoutEnabled: Boolean? = null,
    val dineInEnabled: Boolean? = null,

    // 휴무일
    val daysOff: List<String>? = null,

    // 영업시간(키만 준비)
    val bizHours: Map<String, BusinessHours> = mapOf(
        "MON" to BusinessHours(),
        "TUE" to BusinessHours(),
        "WED" to BusinessHours(),
        "THU" to BusinessHours(),
        "FRI" to BusinessHours(),
        "SAT" to BusinessHours(),
        "SUN" to BusinessHours(),
    ),

    val createdAt: Timestamp? = null
)
