package com.bcu.foodtable.JetpackCompose.Recipe

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun RecipePdfButton(
    onGeneratePdf: (String) -> Unit
) {
    Button(onClick = {
        val dummyHtml = "<html><body><h1>PDF 생성 테스트</h1></body></html>"
        onGeneratePdf(dummyHtml)
    }) {
        Text("PDF 저장")
    }
}
