package com.bcu.foodtable.JetpackCompose.AI

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bcu.foodtable.useful.AIChatting

@Composable
fun ChatBubble(chat: AIChatting) {
    val isAi = chat.content.startsWith("Partner:")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isAi) Arrangement.Start else Arrangement.End
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isAi) Color(0xFFEDEDED) else Color(0xFFDCF8C6),
            modifier = Modifier.padding(4.dp)
        ) {
            Text(
                text = chat.content.removePrefix("Partner:").trim(),
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}
