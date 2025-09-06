package com.bcu.foodtable.ui.merchant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.bcu.foodtable.ui.home.FoodTableTheme
import com.bcu.foodtable.useful.ActivityTransition
import com.bcu.foodtable.ui.merchant.MerchantPage // ← MerchantPage가 Activity일 때 사용
// 만약 MerchantPage가 Compose Screen이라면, 여기 import는 제거하세요.

class MerchantSetupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FoodTableTheme {
                // defaultStoreName을 인텐트로 넘기고 싶다면:
                // val defaultName = intent.getStringExtra("defaultStoreName")
                MerchantSetupScreen(
                    // defaultStoreName = defaultName,
                    onCompleted = { _storeId ->
                        // ✅ 저장 완료 후 이동할 화면을 선택하세요.

                        // (A) 가맹 홈 그리드로 이동
                        ActivityTransition.startStatic(
                            this@MerchantSetupActivity,
                            MerchantPage::class.java
                        )
                        finish()

                        // (B) 바로 상품관리로 이동하고 싶으면 ↓ 사용
                        // ActivityTransition.startStatic(
                        //     this@MerchantSetupActivity,
                        //     ProductManagementActivity::class.java, // 만들었다면
                        //     bundleOf("storeId" to _storeId)
                        // )
                        // finish()
                    }
                )
            }
        }
    }
}
