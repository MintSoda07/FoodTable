package com.bcu.foodtable.ui.merchant

import org.json.JSONObject
import org.json.JSONArray

data class QrItem(
    val pid: String,
    val name: String,
    val price: Long,
    val qty: Int,
    val amount: Long
)

data class QrPayload(
    val type: String,
    val ver: Int,
    val storeId: String,
    val storeName: String,
    val orderId: String,
    val currency: String,
    val subtotal: Long,
    val vatShown: Long,
    val discount: Long,
    val total: Long,
    val coupon: String?,
    val ts: Long,
    val items: List<QrItem>
)

fun parseQrPayloadOrNull(text: String): QrPayload? {
    return try {
        val o = JSONObject(text)

        // 타입 검증
        if (o.optString("type") != "OFFLINE_ORDER_QR") {
            null
        } else {
            val itemsArr = o.optJSONArray("items") ?: JSONArray()
            val items = buildList {
                for (i in 0 until itemsArr.length()) {
                    val it = itemsArr.getJSONObject(i)
                    add(
                        QrItem(
                            pid = it.optString("pid"),
                            name = it.optString("name"),
                            price = it.optLong("price"),
                            qty = it.optInt("qty"),
                            amount = it.optLong("amount")
                        )
                    )
                }
            }

            QrPayload(
                type = o.optString("type"),
                ver = o.optInt("ver", 1),
                storeId = o.optString("storeId"),
                storeName = o.optString("storeName"),
                orderId = o.optString("orderId"),
                currency = o.optString("currency", "KRW"),
                subtotal = o.optLong("subtotal"),
                vatShown = o.optLong("vatShown"),
                discount = o.optLong("discount"),
                total = o.optLong("total"),
                coupon = o.optString("coupon").takeIf { it.isNotBlank() },
                ts = o.optLong("ts"),
                items = items
            )
        }
    } catch (_: Throwable) {
        null
    }
}
