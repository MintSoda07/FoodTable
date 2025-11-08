package com.bcu.foodtable.ui.merchant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.bcu.foodtable.ui.home.FoodTableTheme

class MerchantPage : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FoodTableTheme {
                // ✅ 홈 그리드 → 네비 루트로 교체
                MerchantRoot()
            }
        }
    }
}
