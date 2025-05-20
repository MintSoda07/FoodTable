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

    fun loadIngredients() {
        db.collection("user").document(uid).collection("fridge")
            .get()
            .addOnSuccessListener { snapshot ->
                val items = snapshot.documents.mapNotNull { it.toObject(Ingredient::class.java) }
                ingredientList = items
            }
    }

    fun addIngredient(item: Ingredient, section: String = "냉장", onSuccess: () -> Unit = {}) {
        val itemWithSection = item.copy(section = section)

        db.collection("user").document(uid).collection("fridge")
            .add(itemWithSection)
            .addOnSuccessListener {
                loadIngredients()
                onSuccess()
            }
    }

    fun findRecipesByIngredient(name: String): List<String> {
        // TODO: 여기는 추후 Firestore에서 레시피 ingredients 검색으로 바꿀 수 있음
        val dummyRecipes = mapOf(
            "계란" to listOf("계란말이", "스크램블 에그", "계란국"),
            "당근" to listOf("당근라페", "당근볶음", "야채주먹밥"),
            "소고기" to listOf("불고기", "소고기무국"),
            "상추" to listOf("쌈밥", "상추겉절이"),
            "양파" to listOf("양파볶음", "카레")
        )
        return dummyRecipes[name] ?: listOf("추천 레시피 없음")
    }
}
