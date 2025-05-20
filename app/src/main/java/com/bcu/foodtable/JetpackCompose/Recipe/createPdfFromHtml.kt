package com.bcu.foodtable.JetpackCompose.Recipe

// PdfUtils.kt (같은 파일에 추가하거나 별도 파일에)

import android.app.Activity
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.view.ViewGroup
import android.widget.FrameLayout

fun createPdfFromHtml(activity: Activity, htmlContent: String, fileName: String) {
    val webView = WebView(activity).apply {
        settings.javaScriptEnabled = false
        setBackgroundColor(Color.WHITE) // PDF 배경을 흰색으로 고정
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    // 보이지 않는 레이아웃에 WebView 추가 (렌더링 위해 필요)
    val container = FrameLayout(activity)
    container.addView(webView)
    activity.addContentView(container, ViewGroup.LayoutParams(0, 0)) // 크기 0으로 숨김

    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            // 페이지 렌더링 후 딜레이를 줘서 완전히 그려지도록 함
            Handler(Looper.getMainLooper()).postDelayed({
                createPdfPrintJob(activity, webView, fileName)
            }, 500)
        }
    }

    webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
}

private fun createPdfPrintJob(activity: Activity, webView: WebView, fileName: String) {
    val printManager = activity.getSystemService(Activity.PRINT_SERVICE) as PrintManager
    val printAdapter = webView.createPrintDocumentAdapter(fileName)
    val jobName = "$fileName Document"

    val builder = PrintAttributes.Builder()
        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
        .setResolution(PrintAttributes.Resolution("pdf", "pdf", 600, 600))
        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)

    printManager.print(jobName, printAdapter, builder.build())
}
