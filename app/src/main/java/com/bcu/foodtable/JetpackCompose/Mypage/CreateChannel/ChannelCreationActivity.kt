package com.bcu.foodtable.JetpackCompose.Mypage.CreateChannel


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class ChannelCreationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            com.bcu.foodtable.ui.home.FoodTableTheme {
                ChannelCreationScreen(onFinish = { finish() })
            }
        }
    }
}
