package com.bcu.foodtable.useful

import java.sql.Timestamp

data class PromotionItem(
    val name: String = "",
    val description: String = "",
    val imageres: String = "", // drawable 리소스명 (예: "banner1")
    val link: String = "",
)