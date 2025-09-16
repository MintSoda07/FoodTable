// util/QrBitmap.kt
package com.bcu.foodtable.ui.merchant

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

fun generateQrImageBitmap(content: String, sizePx: Int = 720): ImageBitmap {
    val hints = mapOf(
        EncodeHintType.CHARACTER_SET to "UTF-8",
        EncodeHintType.MARGIN to 0
    )
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val black = 0xFF000000.toInt()
    val white = 0xFFFFFFFF.toInt()
    val pixels = IntArray(sizePx * sizePx)
    var idx = 0
    for (y in 0 until sizePx) {
        for (x in 0 until sizePx) {
            pixels[idx++] = if (matrix[x, y]) black else white
        }
    }
    bmp.setPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)
    return bmp.asImageBitmap()
}
