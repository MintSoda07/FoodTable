package com.bcu.foodtable.ui.merchant

import com.google.firebase.Timestamp

data class BusinessHours(
    val open: String = "09:00",
    val close: String = "21:00",
    val closed: Boolean = false
)

data class StoreProfile(
    val storeId: String = "",
    val storeName: String = "",
    val ownerUid: String = "",
    val phone: String = "",
    val address: String = "",
    val category: String = "미분류",
    val description: String = "",
    val minOrderPrice: Long = 0,
    val taxPercent: Double = 10.0,
    val onlineOrderEnabled: Boolean = true,
    val takeoutEnabled: Boolean = true,
    val dineInEnabled: Boolean = true,
    val openNow: Boolean = false,
    val status: String = "ACTIVE", // ACTIVE / PAUSED / CLOSED
    val daysOff: List<String> = emptyList(),
    val bizHours: Map<String, BusinessHours> = defaultBizHours(),
    val createdAt: Timestamp? = null,
    val logoUrl: String = ""
)

fun defaultBizHours(): Map<String, BusinessHours> = mapOf(
    "MON" to BusinessHours(),
    "TUE" to BusinessHours(),
    "WED" to BusinessHours(),
    "THU" to BusinessHours(),
    "FRI" to BusinessHours(),
    "SAT" to BusinessHours(open = "10:00", close = "20:00"),
    "SUN" to BusinessHours(open = "10:00", close = "20:00", closed = true)
)
