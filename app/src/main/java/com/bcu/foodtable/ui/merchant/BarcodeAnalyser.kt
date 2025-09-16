package com.bcu.foodtable.ui.merchant

import android.annotation.SuppressLint
import android.graphics.ImageFormat
import android.media.Image
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

class BarcodeAnalyser(
    private val onBarcodeText: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage: Image = imageProxy.image ?: run {
            imageProxy.close(); return
        }
        if (imageProxy.format != ImageFormat.YUV_420_888) {
            imageProxy.close(); return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    // QR CODE or other formats can carry rawValue
                    val raw = barcode.rawValue
                    if (!raw.isNullOrBlank()) {
                        onBarcodeText(raw)
                        break
                    }
                }
            }
            .addOnFailureListener {
                // ignore, keep scanning
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
}
