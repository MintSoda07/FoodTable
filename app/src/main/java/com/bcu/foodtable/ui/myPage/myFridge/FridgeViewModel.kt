package com.bcu.foodtable.ui.myPage.myFridge

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

class FridgeViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val uid = Firebase.auth.currentUser?.uid ?: ""

    var ingredientList by mutableStateOf<List<Ingredient>>(emptyList())
        private set

    // 재료 로딩
    fun loadIngredients() {
        if (uid == null) return

        db.collection("user").document(uid).collection("fridge")
            .get()
            .addOnSuccessListener { snapshot ->
                val items = snapshot.documents.mapNotNull { doc ->
                    val item = doc.toObject(Ingredient::class.java)
                    // 문서 ID를 item에 추가로 저장하려면 여기에 처리 (예: item.id = doc.id)
                    item
                }
                ingredientList = items
            }
            .addOnFailureListener {
                println(" loadIngredients 실패: ${it.message}")
            }
    }
    // 재료 추가
    fun addIngredient(item: Ingredient, section: String = "냉장", onSuccess: () -> Unit = {}) {
        if (uid == null) return

        val itemWithSection = item.copy(section = section)

        db.collection("user").document(uid).collection("fridge")
            .add(itemWithSection)
            .addOnSuccessListener {
                loadIngredients()
                onSuccess()
            }
            .addOnFailureListener {
                println(" addIngredient 실패: ${it.message}")
            }
    }
    // 재료 이동
    fun updateIngredientSection(id: String, newSection: String) {
        val docRef = db.collection("user").document(uid).collection("fridge")
        docRef.whereEqualTo("id", id).get().addOnSuccessListener { snapshot ->
            snapshot.documents.firstOrNull()?.reference?.update("section", newSection)
        }
    }
    // 레시피 찾기 예시
    fun findRecipesByIngredient(name: String): List<String> {
        // TODO: 여기는 추후 Firestore에서 레시피 ingredients 검색으로 바꿀 수 있음
        val normalized = name.trim().lowercase()
        val dummyRecipes = mapOf(
            "계란" to listOf("계란말이", "스크램블 에그", "계란국"),
            "당근" to listOf("당근라페", "당근볶음", "야채주먹밥"),
            "소고기" to listOf("불고기", "소고기무국"),
            "상추" to listOf("쌈밥", "상추겉절이"),
            "양파" to listOf("양파볶음", "카레")
        )
        return dummyRecipes.entries.firstOrNull {
            it.key == normalized
        }?.value ?: listOf("추천 레시피 없음")
    }
}
