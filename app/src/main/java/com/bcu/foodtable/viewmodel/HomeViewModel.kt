package com.bcu.foodtable.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bcu.foodtable.useful.RecipeItem
import com.bcu.foodtable.useful.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class HomeViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // 🔄 레시피 리스트 상태
    private val _recipes = MutableStateFlow<List<RecipeItem>>(emptyList())
    val recipes: StateFlow<List<RecipeItem>> = _recipes

    // 📂 카테고리 선택 상태
    private val _selectedCategory = MutableStateFlow("종류")
    val selectedCategory: StateFlow<String> = _selectedCategory

    // 🏷️ 선택된 태그 상태
    private val _selectedTags = MutableStateFlow<List<String>>(emptyList())
    val selectedTags: StateFlow<List<String>> = _selectedTags

    private val _currentUser = MutableStateFlow<User>(User())
    val currentUser: StateFlow<User> = _currentUser

    init {
        loadCurrentUser()
        loadRecipes()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                val userDoc = db.collection("users").document(userId).get().await()
                userDoc.toObject(User::class.java)?.let { user ->
                    _currentUser.value = user.copy(uid = userId)
                }
            } catch (e: Exception) {
                // 에러 처리
            }
        }
    }

    fun loadRecipes() {
        viewModelScope.launch {
            try {
                Log.d("HomeViewModel", "Starting to load recipes...")
                
                // Firestore 인스턴스 상태 확인
                if (db == null) {
                    Log.e("HomeViewModel", "Firestore instance is null")
                    return@launch
                }

                // 컬렉션 참조 생성 및 로깅
                val recipesRef = db.collection("recipe")
                Log.d("HomeViewModel", "Accessing collection: ${recipesRef.path}")

                try {
                    // 먼저 컬렉션이 존재하는지 확인
                    val metadata = recipesRef.get().await()
                    Log.d("HomeViewModel", "Collection metadata: exists=${metadata.metadata.isFromCache}, size=${metadata.size()}")

                    if (metadata.isEmpty) {
                        Log.w("HomeViewModel", "No documents found in recipe collection")
                        _recipes.value = emptyList()
                        return@launch
                    }

                    // 문서 가져오기
                    val snapshot = metadata
                    Log.d("HomeViewModel", "Firestore query completed. Document count: ${snapshot.documents.size}")
                    
                    val list = snapshot.documents.mapNotNull { doc -> 
                        try {
                            Log.d("HomeViewModel", "Processing document: ${doc.id}")
                            Log.d("HomeViewModel", "Document data: ${doc.data}")
                            
                            val recipe = doc.toObject(RecipeItem::class.java)
                            recipe?.also { r ->
                                Log.d("HomeViewModel", "Successfully mapped document to recipe: ${r.name}")
                            } ?: run {
                                Log.e("HomeViewModel", "Failed to map document to RecipeItem: ${doc.id}")
                            }
                            recipe
                        } catch (e: Exception) {
                            Log.e("HomeViewModel", "Error processing document ${doc.id}: ${e.message}")
                            null
                        }
                    }
                    
                    _recipes.value = list
                    Log.d("HomeViewModel", "Total recipes loaded: ${list.size}")
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Error accessing Firestore: ${e.message}")
                    e.printStackTrace()
                    _recipes.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Fatal error in loadRecipes: ${e.message}")
                e.printStackTrace()
                _recipes.value = emptyList()
            }
        }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun toggleTag(tag: String) {
        val current = _selectedTags.value.toMutableList()
        if (current.contains(tag)) {
            current.remove(tag)
        } else {
            current.add(tag)
        }
        _selectedTags.value = current
    }

    fun removeTag(tag: String) {
        _selectedTags.value = _selectedTags.value.filterNot { it == tag }
    }
}
