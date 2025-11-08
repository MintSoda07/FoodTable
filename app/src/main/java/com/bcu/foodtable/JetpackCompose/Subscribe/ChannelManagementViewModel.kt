package com.bcu.foodtable.JetpackCompose.Subscribe

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bcu.foodtable.useful.RecipeItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

data class ChannelStats(
    val totalViews: Int = 0,
    val totalLikes: Int = 0,
    val totalPurchases: Int = 0,
    val totalUploads: Int = 0,
    val totalRevenue: Float = 0f,
    val percentChange: List<Float> = List(5) { 0f },

    // 월별 (yyyy-MM 순정렬)
    val monthlyLabels: List<String> = emptyList(),
    val monthlyViews: List<String> = emptyList(),
    val monthlyLikes: List<String> = emptyList(),
    val monthlyPurchases: List<String> = emptyList(),
    val monthlyUploads: List<String> = emptyList(),
    val monthlyRevenue: List<String> = emptyList(),

    // 연도별 (yyyy 순정렬)
    val yearlyLabels: List<String> = emptyList(),
    val yearlyViews: List<String> = emptyList(),
    val yearlyLikes: List<String> = emptyList(),
    val yearlyPurchases: List<String> = emptyList(),
    val yearlyUploads: List<String> = emptyList(),
    val yearlyRevenue: List<String> = emptyList(),

    // 일별 (yyyy-MM-dd 최근 7일)
    val dailyLabels: List<String> = emptyList(),
    val dailyViews: List<String> = emptyList(),
    val dailyLikes: List<String> = emptyList(),
    val dailyPurchases: List<String> = emptyList(),
    val dailyUploads: List<String> = emptyList(),
    val dailyRevenue: List<String> = emptyList(),

    // 진행률, 파이차트용: 각 카테고리별 최근 값(일간/월간/연간에서 뽑아씀)
    val dailyMetrics: List<List<String>> = List(5) { emptyList() },
    val monthlyMetrics: List<List<String>> = List(5) { emptyList() },
    val yearlyMetrics: List<List<String>> = List(5) { emptyList() },

    // 목표치
    val maxTargets: List<Float> = listOf(1000f, 100f, 20f, 10f, 50000f)
)

class ChannelViewModel : ViewModel() {
    var isLoading by mutableStateOf(true)
    var stats by mutableStateOf(ChannelStats())

    fun loadChannelStats(channelName: String) {
        viewModelScope.launch {
            isLoading = true

            val db = FirebaseFirestore.getInstance()
            val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val monthFmt = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val yearFmt = SimpleDateFormat("yyyy", Locale.getDefault())

            val viewsByDay = sortedMapOf<String, Int>()
            val likesByDay = sortedMapOf<String, Int>()
            val purchasesByDay = sortedMapOf<String, Int>()
            val uploadsByDay = sortedMapOf<String, Int>()
            val revenueByDay = sortedMapOf<String, Float>()

            val viewsByMonth = sortedMapOf<String, Int>()
            val likesByMonth = sortedMapOf<String, Int>()
            val purchasesByMonth = sortedMapOf<String, Int>()
            val uploadsByMonth = sortedMapOf<String, Int>()
            val revenueByMonth = sortedMapOf<String, Float>()

            val viewsByYear = sortedMapOf<String, Int>()
            val likesByYear = sortedMapOf<String, Int>()
            val purchasesByYear = sortedMapOf<String, Int>()
            val uploadsByYear = sortedMapOf<String, Int>()
            val revenueByYear = sortedMapOf<String, Float>()

            val recipeDocs = db.collection("recipe")
                .whereEqualTo("contained_channel", channelName)
                .get().await()
            val recipes = recipeDocs.toObjects(RecipeItem::class.java)

            var totalViews = 0
            var totalLikes = 0
            var totalPurchases = 0
            var totalUploads = 0
            var totalRevenue = 0f

            recipes.forEach { recipe ->
                val date = recipe.date.toDate()
                val dayKey = dateFmt.format(date)
                val monthKey = monthFmt.format(date)
                val yearKey = yearFmt.format(date)

                totalViews += recipe.clicked
                totalLikes += recipe.likes
                val purchCount = if (recipe.isPurchased) 1 else 0
                totalPurchases += purchCount
                totalUploads += 1
                val revEngagement = recipe.likes * 0.15f + recipe.clicked * 0.05f
                val rev = revEngagement + (recipe.cost * purchCount)
                totalRevenue += rev

                // 일별
                viewsByDay[dayKey] = (viewsByDay[dayKey] ?: 0) + recipe.clicked
                likesByDay[dayKey] = (likesByDay[dayKey] ?: 0) + recipe.likes
                purchasesByDay[dayKey] = (purchasesByDay[dayKey] ?: 0) + purchCount
                uploadsByDay[dayKey] = (uploadsByDay[dayKey] ?: 0) + 1
                revenueByDay[dayKey] = (revenueByDay[dayKey] ?: 0f) + rev

                // 월별
                viewsByMonth[monthKey] = (viewsByMonth[monthKey] ?: 0) + recipe.clicked
                likesByMonth[monthKey] = (likesByMonth[monthKey] ?: 0) + recipe.likes
                purchasesByMonth[monthKey] = (purchasesByMonth[monthKey] ?: 0) + purchCount
                uploadsByMonth[monthKey] = (uploadsByMonth[monthKey] ?: 0) + 1
                revenueByMonth[monthKey] = (revenueByMonth[monthKey] ?: 0f) + rev

                // 연도별
                viewsByYear[yearKey] = (viewsByYear[yearKey] ?: 0) + recipe.clicked
                likesByYear[yearKey] = (likesByYear[yearKey] ?: 0) + recipe.likes
                purchasesByYear[yearKey] = (purchasesByYear[yearKey] ?: 0) + purchCount
                uploadsByYear[yearKey] = (uploadsByYear[yearKey] ?: 0) + 1
                revenueByYear[yearKey] = (revenueByYear[yearKey] ?: 0f) + rev
            }

            // 정렬 및 최근 N개 슬라이스
            val recentDays = viewsByDay.keys.toList().takeLast(7)
            val dailyViews = recentDays.map { (viewsByDay[it] ?: 0).toString() }
            val dailyLikes = recentDays.map { (likesByDay[it] ?: 0).toString() }
            val dailyPurchases = recentDays.map { (purchasesByDay[it] ?: 0).toString() }
            val dailyUploads = recentDays.map { (uploadsByDay[it] ?: 0).toString() }
            val dailyRevenue = recentDays.map { (revenueByDay[it] ?: 0f).toString() }

            val recentMonths = viewsByMonth.keys.toList().takeLast(5)
            val monthlyViews = recentMonths.map { (viewsByMonth[it] ?: 0).toString() }
            val monthlyLikes = recentMonths.map { (likesByMonth[it] ?: 0).toString() }
            val monthlyPurchases = recentMonths.map { (purchasesByMonth[it] ?: 0).toString() }
            val monthlyUploads = recentMonths.map { (uploadsByMonth[it] ?: 0).toString() }
            val monthlyRevenue = recentMonths.map { (revenueByMonth[it] ?: 0f).toString() }

            val recentYears = viewsByYear.keys.toList().takeLast(3)
            val yearlyViews = recentYears.map { (viewsByYear[it] ?: 0).toString() }
            val yearlyLikes = recentYears.map { (likesByYear[it] ?: 0).toString() }
            val yearlyPurchases = recentYears.map { (purchasesByYear[it] ?: 0).toString() }
            val yearlyUploads = recentYears.map { (uploadsByYear[it] ?: 0).toString() }
            val yearlyRevenue = recentYears.map { (revenueByYear[it] ?: 0f).toString() }

            // 증감률: 월간 데이터 기준, (직전-이전)/이전
            fun percentChange(list: List<String>): Float {
                if (list.size < 2) return 0f
                val last = list[list.size - 1].toFloatOrNull() ?: 0f
                val prev = list[list.size - 2].toFloatOrNull() ?: 0f
                return if (prev == 0f) 0f else ((last - prev) / prev * 100f)
            }
            val percentChangeList = listOf(
                percentChange(monthlyViews),
                percentChange(monthlyLikes),
                percentChange(monthlyPurchases),
                percentChange(monthlyUploads),
                percentChange(monthlyRevenue)
            )

            stats = ChannelStats(
                totalViews = totalViews,
                totalLikes = totalLikes,
                totalPurchases = totalPurchases,
                totalUploads = totalUploads,
                totalRevenue = totalRevenue,
                percentChange = percentChangeList,

                dailyLabels = recentDays.toList(),
                dailyViews = dailyViews,
                dailyLikes = dailyLikes,
                dailyPurchases = dailyPurchases,
                dailyUploads = dailyUploads,
                dailyRevenue = dailyRevenue,
                dailyMetrics = listOf(dailyViews, dailyLikes, dailyPurchases, dailyUploads, dailyRevenue),

                monthlyLabels = recentMonths.toList(),
                monthlyViews = monthlyViews,
                monthlyLikes = monthlyLikes,
                monthlyPurchases = monthlyPurchases,
                monthlyUploads = monthlyUploads,
                monthlyRevenue = monthlyRevenue,
                monthlyMetrics = listOf(monthlyViews, monthlyLikes, monthlyPurchases, monthlyUploads, monthlyRevenue),

                yearlyLabels = recentYears.toList(),
                yearlyViews = yearlyViews,
                yearlyLikes = yearlyLikes,
                yearlyPurchases = yearlyPurchases,
                yearlyUploads = yearlyUploads,
                yearlyRevenue = yearlyRevenue,
                yearlyMetrics = listOf(yearlyViews, yearlyLikes, yearlyPurchases, yearlyUploads, yearlyRevenue),

                maxTargets = listOf(1000f, 100f, 20f, 10f, 50000f)
            )
            isLoading = false
        }
    }
}
