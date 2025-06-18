package com.bcu.foodtable.JetpackCompose.Subscribe

import android.util.Log
import androidx.annotation.RawRes
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.compose.*
import com.bcu.foodtable.R
import com.bcu.foodtable.useful.RecipeItem
import com.github.tehras.charts.bar.*
import com.github.tehras.charts.bar.renderer.label.SimpleValueDrawer
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "ChannelViewModel"

// ----- 데이터 클래스 -----

data class ChannelStats(
    val subscribers: Int = 0,
    val totalRecipes: Int = 0,
    val views: Int = 0,
    val likes: Int = 0,
    val purchases: Int = 0,
    val revenue: Float = 0f,
    val monthlyViews: Map<String, Int> = emptyMap(),
    val monthlyLikes: Map<String, Int> = emptyMap(),
    val monthlyPurchases: Map<String, Int> = emptyMap(),
    val monthlyUploads: Map<String, Int> = emptyMap(),
    val monthlyRevenue: Map<String, Float> = emptyMap()
)

// ----- ViewModel -----

class ChannelViewModel : ViewModel() {
    var isLoading by mutableStateOf(true)
    var stats by mutableStateOf(ChannelStats())

    fun loadChannelStats(channelName: String) {
        viewModelScope.launch {
            val db = FirebaseFirestore.getInstance()
            val monthFmt = SimpleDateFormat("yyyy-MM", Locale.getDefault())

            val viewsMap = mutableMapOf<String, Int>()
            val likesMap = mutableMapOf<String, Int>()
            val purchasesMap = mutableMapOf<String, Int>()
            val uploadsMap = mutableMapOf<String, Int>()
            val revenueMap = mutableMapOf<String, Float>()

            val recipeDocs = db.collection("recipe")
                .whereEqualTo("contained_channel", channelName)
                .get().await()
            val recipes = recipeDocs.toObjects(RecipeItem::class.java)

            var totalViews = 0
            var totalLikes = 0
            var totalPurchases = 0
            var totalRevenue = 0f

            recipes.forEach { recipe ->
                val date = recipe.date.toDate()
                val monthKey = monthFmt.format(date)

                totalViews += recipe.clicked
                totalLikes += recipe.likes
                val purchCount = if (recipe.isPurchased) 1 else 0
                totalPurchases += purchCount

                val revEngagement = recipe.likes * 0.15f + recipe.clicked * 0.05f
                val rev = revEngagement + (recipe.cost * purchCount)
                totalRevenue += rev

                viewsMap[monthKey] = viewsMap.getOrDefault(monthKey, 0) + recipe.clicked
                likesMap[monthKey] = likesMap.getOrDefault(monthKey, 0) + recipe.likes
                purchasesMap[monthKey] = purchasesMap.getOrDefault(monthKey, 0) + purchCount
                uploadsMap[monthKey] = uploadsMap.getOrDefault(monthKey, 0) + 1
                revenueMap[monthKey] = revenueMap.getOrDefault(monthKey, 0f) + rev
            }

            val channelDoc = db.collection("channel")
                .whereEqualTo("name", channelName)
                .limit(1).get().await().documents.firstOrNull()
            val subscribers = channelDoc?.getLong("subscribers")?.toInt() ?: 0

            stats = ChannelStats(
                subscribers = subscribers,
                totalRecipes = recipes.size,
                views = totalViews,
                likes = totalLikes,
                purchases = totalPurchases,
                revenue = totalRevenue,
                monthlyViews = viewsMap.toSortedMap(),
                monthlyLikes = likesMap.toSortedMap(),
                monthlyPurchases = purchasesMap.toSortedMap(),
                monthlyUploads = uploadsMap.toSortedMap(),
                monthlyRevenue = revenueMap.toSortedMap()
            )
            isLoading = false
        }
    }
}
