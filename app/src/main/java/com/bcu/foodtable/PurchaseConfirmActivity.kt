package com.bcu.foodtable

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.portone.sdk.android.PortOne
import io.portone.sdk.android.payment.*
import io.portone.sdk.android.type.*
import java.util.UUID

class PurchaseConfirmActivity : AppCompatActivity() {
    // 결제 완료/실패 이후 응답을 처리 하기 위한 ResultLauncher 생성
    private val paymentActivityResultLauncher =
        PortOne.registerForPaymentActivity(this, callback = object :
            PaymentCallback {
            override fun onSuccess(response: PaymentResponse.Success) {
                AlertDialog.Builder(this@PurchaseConfirmActivity)
                    .setTitle("결제 성공")
                    .setMessage(response.toString())
                    .show()
            }

            override fun onFail(response: PaymentResponse.Fail) {
                AlertDialog.Builder(this@PurchaseConfirmActivity)
                    .setTitle("결제 실패")
                    .setMessage(response.toString())
                    .show()
            }

        })
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_purchase_confirm)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val costStr = intent.getStringExtra("price")
        val cost: Long = costStr?.toLongOrNull() ?: 0L
        Log.d("Purchase", "BEFORE STR : $costStr , AFTER LONG : $cost")

        PortOne.requestPayment(
            this,
            request = PaymentRequest(
                storeId = "store-38616698-cf3e-4364-9073-494e2127e935",
                channelKey = "channel-key-14ab4c31-cba3-447a-9543-941396495fd9",
                paymentId = "babsang-${UUID.randomUUID()}",
                orderName = "밥상친구 소금 $cost 개",
                amount = Amount(total = cost , currency = Currency.KRW), // 금액
                method = PaymentMethod.Card() // 결제수단 관련 정보
            ),
            resultLauncher = paymentActivityResultLauncher
        )
    }
}