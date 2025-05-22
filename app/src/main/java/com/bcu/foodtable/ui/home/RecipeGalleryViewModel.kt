package com.bcu.foodtable.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bcu.foodtable.useful.GalleryItem
import com.bcu.foodtable.useful.RecipeItem
import com.bcu.foodtable.useful.UserManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class RecipeGalleryViewModel : ViewModel() {

    private val _galleryItems = MutableStateFlow<List<GalleryItem>>(emptyList())
    val galleryItems: StateFlow<List<GalleryItem>> = _galleryItems

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    /**
     * 뷰에서 호출 필요: LaunchedEffect(Unit) { viewModel.loadGalleryItems() }
     */
    private val _loadFailed = MutableStateFlow(false)
    val loadFailed: StateFlow<Boolean> = _loadFailed

    fun loadGalleryItems() {
        viewModelScope.launch {
            _isLoading.value = true
            _loadFailed.value = false
            try {
                _galleryItems.value = fetchGalleryItems()
            } catch (e: Exception) {
                Log.e("Gallery", "로딩 실패", e)
                _galleryItems.value = emptyList()
                _loadFailed.value = true
            } finally {
                _isLoading.value = false
            }
        }
    }


    /**
     * Firestore에서 유저의 갤러리 아이템과 레시피 정보를 병렬로 가져와 구성
     */
    private suspend fun fetchGalleryItems(): List<GalleryItem> = kotlinx.coroutines.coroutineScope {
        val firestore = FirebaseFirestore.getInstance()
        val userId = UserManager.getUser()?.uid ?: return@coroutineScope emptyList()

        try {
            // 1. recipe_follow에서 유저의 팔로우 정보 가져오기
            val snapshot = firestore.collection("recipe_follow")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            // 2. recipeId -> GalleryItem 매핑
            val baseItems = snapshot.documents.mapNotNull { doc ->
                val item = doc.toObject(GalleryItem::class.java)
                item?.recipeId?.let { recipeId -> recipeId to item }
            }

            // 3. 각 recipeId에 대해 recipe 상세 정보 병렬로 요청
            val enrichedItems = baseItems.map { (recipeId, item) ->
                async {
                    try {
                        val recipeDoc = firestore.collection("recipe")
                            .document(recipeId)
                            .get()
                            .await()

                        val recipe = recipeDoc.toObject(RecipeItem::class.java)
                        if (recipe != null) {
                            // 4. recipe가 존재하면 GalleryItem에 name/image 채워서 반환
                            item.copy(
                                name = recipe.name,
                                image = recipe.imageResId
                            )
                        } else {
                            // recipe가 없으면 제외
                            null
                        }
                    } catch (e: Exception) {
                        Log.e("GalleryFetch", "레시피 정보 불러오기 실패: $recipeId", e)
                        null
                    }
                }
            }.awaitAll().filterNotNull()

            enrichedItems
        } catch (e: Exception) {
            Log.e("GalleryFetch", "갤러리 항목 불러오기 실패", e)
            emptyList()
        }
    }

    /**
     * 특정 레시피의 그룹 ID를 Firestore 및 StateFlow에 반영
     */
    fun updateItemGroup(recipeId: String, newGroupId: String) {
        viewModelScope.launch {
            try {
                val firestore = FirebaseFirestore.getInstance()
                val docs = firestore.collection("recipe_follow")
                    .whereEqualTo("recipeId", recipeId)
                    .get()
                    .await()

                docs.documents.forEach { doc ->
                    firestore.collection("recipe_follow")
                        .document(doc.id)
                        .update("groupId", newGroupId)
                        .await()
                }

                Log.d("GalleryUpdate", "그룹 ID 업데이트 완료: $recipeId -> $newGroupId")

                _galleryItems.value = _galleryItems.value.map {
                    if (it.recipeId == recipeId) it.copy(groupId = newGroupId) else it
                }
            } catch (e: Exception) {
                Log.e("GalleryUpdate", "그룹 ID 업데이트 실패", e)
            }
        }
    }
}
