package com.bcu.foodtable.ui.myPage

import android.health.connect.datatypes.HeartRateRecord
import java.time.Instant

data class HeartRateRecord(
    val samples: List<HeartRateRecord.HeartRateSample>,
    val startTime: Instant,
    val endTime: Instant

)