package com.bcu.foodtable.JetpackCompose.Social

import androidx.lifecycle.ViewModel

// 1) ViewModel 클래스 정의
class MatzipViewModel : ViewModel() {
    // 아까 VectorMapViewModel 과는 별개로
    // 'matzip' 화면에서 사용할 상태/로직을 여기에 구현
    val restaurants = listOf(
        Restaurant("홍콩반점 점심특선", 37.5665, 126.9780),
        Restaurant("갈비명가",           37.5651, 126.9895),
        Restaurant("카레왕국",           37.5704, 126.9820),
    )
}

data class Restaurant(
    val name: String,
    val latitude: Double,
    val longitude: Double
)