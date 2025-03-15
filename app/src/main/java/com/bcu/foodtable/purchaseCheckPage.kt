package com.bcu.foodtable

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bcu.foodtable.useful.ApiKeyManager
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Callback
import retrofit2.Response
import java.util.UUID

class purchaseCheckPage : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_purchase_check_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }



        // requestPayment()
    }
}

fun requestPayment(token: String, storeId: String, amount: Int,channelKey: String) {
    val request = PortOnePaymentRequest(
            storeId = storeId,
        channelKey = channelKey,
        paymentId = "payment-${UUID.randomUUID()}",
        orderName = "소금 충전",
        totalAmount = amount,
        payMethod = "CARD"
    )

    RetrofitClient.instance.preparePayment(token, request).enqueue(object : Callback<Unit> {
        override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
            if (response.isSuccessful) {
                println("결제 사전 등록 성공!")
            } else {
                println("결제 사전 등록 실패: ${response.errorBody()?.string()}")
            }
        }

        override fun onFailure(call: Call<Unit>, t: Throwable) {
            println("네트워크 오류: ${t.message}")
        }
    })
}

fun getPaymentResult(token: String, impUid: String) {
    RetrofitClient.instance.getPaymentResult(token, impUid).enqueue(object : Callback<PaymentResponse> {
        override fun onResponse(call: Call<PaymentResponse>, response: Response<PaymentResponse>) {
            if (response.isSuccessful) {
                val result = response.body()
                println("결제 상태: ${result?.status}")
            } else {
                println("결제 조회 실패: ${response.errorBody()?.string()}")
            }
        }

        override fun onFailure(call: Call<PaymentResponse>, t: Throwable) {
            println("네트워크 오류: ${t.message}")
        }
    })
}

// PortOne 결제 요청 모델
data class PortOnePaymentRequest(
    val storeId: String,
    val channelKey: String,
    val paymentId: String,
    val orderName: String,
    val totalAmount: Int,
    val currency: String = "CURRENCY_KRW",
    val payMethod: String
)

// API 응답 모델
data class PaymentResponse(
    val imp_uid: String,  // PortOne 결제 UID
    val merchant_uid: String,
    val amount: Int,
    val status: String
)

// Retrofit 인터페이스
interface PortOneApiService {

    // 1. 결제 요청 (서버에 사전 등록)
    @POST("https://api.iamport.kr/payments/prepare")
    fun preparePayment(
        @Header("Authorization") token: String,
        @Body request: PortOnePaymentRequest
    ): Call<Unit>

    // 2. 결제 내역 조회
    @GET("https://api.iamport.kr/payments/{imp_uid}")
    fun getPaymentResult(
        @Header("Authorization") token: String,
        @Path("imp_uid") impUid: String
    ): Call<PaymentResponse>
}
object RetrofitClient {
    private val client = OkHttpClient.Builder().build()

    val instance: PortOneApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.iamport.kr/")  // PortOne API 서버
            .addConverterFactory(GsonConverterFactory.create())  // JSON 변환
            .client(client)
            .build()
            .create(PortOneApiService::class.java)
    }
}