// MultiShopPriceSearchActivity.kt
package com.bcu.foodtable.JetpackCompose

// MultiShopPriceSearchActivity.kt
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.bcu.foodtable.JetpackCompose.MultiShopPriceSearchScreen

class MultiShopPriceSearchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MultiShopPriceSearchScreen(intent)
        }
    }
}
