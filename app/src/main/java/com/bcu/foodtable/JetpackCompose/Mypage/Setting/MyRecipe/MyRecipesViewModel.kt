package com.bcu.foodtable.JetpackCompose.Mypage.Setting.MyRecipe

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

data class RecipeSummary(
    val id: String,
    val title: String,
    val imageUrl: String?,
    val categories: List<String>,
    val estimatedCaloriesText: String?, // "650 kcal" 같은 원본 표시용
    val estimatedCalories: Int?,        // 정렬용 숫자(kcal)
    val durationMin: Int?,              // 정렬용 숫자(분)
    val dateText: String?,              // 표시용
    val sortDate: Timestamp?,           // 정렬용 (createdAt or date)
    val containedChannel: String? = null
)

class MyRecipesViewModel : ViewModel() {
    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _items = MutableStateFlow<List<RecipeSummary>>(emptyList())
    val items: StateFlow<List<RecipeSummary>> = _items

    init { fetchMyChannelRecipes() }

    /** 내 채널(owner == uid)의 name 목록을 만든 뒤,
     *  recipe.contained_channel ∈ names 인 문서만 가져온다. (10개 단위 배치 whereIn)
     */
    fun fetchMyChannelRecipes() = viewModelScope.launch {
        val uid = auth.currentUser?.uid ?: run {
            Log.w("MyRecipesVM", "No currentUser")
            return@launch
        }
        _loading.value = true
        Log.d("MyRecipesVM", "fetch channels for uid=$uid")

        db.collection("channel")
            .whereEqualTo("owner", uid)
            .get()
            .addOnSuccessListener { chSnap ->
                val channelNames = chSnap.documents.mapNotNull { it.getString("name") }.distinct()
                Log.d("MyRecipesVM", "my channels=${channelNames.size} $channelNames")

                if (channelNames.isEmpty()) {
                    _items.value = emptyList()
                    _loading.value = false
                    return@addOnSuccessListener
                }

                // whereIn 은 최대 10개 까지 → 10개 단위로 쿼리 병렬 수행
                val batches = channelNames.chunked(10)
                val collector = mutableListOf<RecipeSummary>()
                var pending = batches.size
                fun finish() {
                    // 중복 제거(id 기준)
                    val dedup = collector
                        .distinctBy { it.id }
                        .sortedWith(
                            compareByDescending<RecipeSummary> { it.sortDate ?: Timestamp(0, 0) }
                                .thenByDescending { it.id }
                        )
                    _items.value = dedup
                    _loading.value = false
                    Log.d("MyRecipesVM", "recipes total=${dedup.size}")
                }

                batches.forEach { group ->
                    val q = if (group.size == 1) {
                        db.collection("recipe").whereEqualTo("contained_channel", group.first())
                    } else {
                        db.collection("recipe").whereIn("contained_channel", group)
                    }

                    q.get()
                        .addOnSuccessListener { rSnap ->
                            rSnap.documents.forEach { d ->
                                val id = d.getString("id") ?: d.id
                                val title = (d.getString("name")
                                    ?: d.getString("title")
                                    ?: d.getString("description")
                                    ?: "제목 없음").trim()

                                val image = d.getString("imageResId")

                                // 카테고리는 스키마가 c_categories / C_categories 혼재 가능
                                val cats = (d.get("c_categories") as? List<*>)?.filterIsInstance<String>()
                                    ?: (d.get("C_categories") as? List<*>)?.filterIsInstance<String>()
                                    ?: emptyList()

                                val kcalText = d.getString("estimatedCalories")
                                val kcal = parseKcal(kcalText)

                                val durationMin = (d.getLong("duration") ?: d.getLong("cookingDuration") ?: 0L)
                                    .toInt().takeIf { it > 0 }

                                val dateAny = d.get("date")
                                val dateText = formatAnyDate(dateAny)
                                val sortDate = d.getTimestamp("createdAt")
                                    ?: (dateAny as? com.google.firebase.Timestamp)

                                val contained = d.getString("contained_channel")

                                collector += RecipeSummary(
                                    id = id,
                                    title = title,
                                    imageUrl = image,
                                    categories = cats,
                                    estimatedCaloriesText = kcalText,
                                    estimatedCalories = kcal,
                                    durationMin = durationMin,
                                    dateText = dateText,
                                    sortDate = sortDate,
                                    containedChannel = contained
                                )
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("MyRecipesVM", "whereIn batch failed: ${e.message}")
                        }
                        .addOnCompleteListener {
                            pending--
                            if (pending == 0) finish()
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("MyRecipesVM", "channel query failed: ${e.message}")
                _items.value = emptyList()
                _loading.value = false
            }
    }
}

/** "650 kcal" → 650 */
private fun parseKcal(text: String?): Int? {
    if (text.isNullOrBlank()) return null
    val m = Pattern.compile("(\\d{1,6})").matcher(text.replace(",", ""))
    return if (m.find()) m.group(1)?.toIntOrNull() else null
}

/** date: String / Timestamp / Long / Date 모두 대응 */
private fun formatAnyDate(any: Any?): String? {
    val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return when (any) {
        null -> null
        is String -> any
        is Timestamp -> fmt.format(any.toDate())
        is Date -> fmt.format(any)
        is Long -> fmt.format(Date(any))
        is Int -> fmt.format(Date(any.toLong()))
        else -> any.toString()
    }
}
