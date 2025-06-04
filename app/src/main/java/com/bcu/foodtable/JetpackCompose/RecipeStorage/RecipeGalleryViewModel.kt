package com.bcu.foodtable.JetpackCompose.RecipeStorage

import ads_mobile_sdk.db
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bcu.foodtable.useful.GalleryItem
import com.bcu.foodtable.useful.RecipeItem
import com.bcu.foodtable.useful.UserManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID


class RecipeGalleryViewModel : ViewModel() {

    private val _galleryItems = MutableStateFlow<List<GalleryItem>>(emptyList())
    val galleryItems: StateFlow<List<GalleryItem>> = _galleryItems
    val db = FirebaseFirestore.getInstance()
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading
    private var groupCounter = 1
    /**
     뷰에서 호출 필요: LaunchedEffect(Unit) { viewModel.loadGalleryItems() }
     */
    private val _loadFailed = MutableStateFlow(false)
    val loadFailed: StateFlow<Boolean> = _loadFailed

    fun loadGalleryItems() {
        viewModelScope.launch {
            _isLoading.value = true
            _loadFailed.value = false
            try {
                // 🔸 1. 그룹화된 레시피 먼저 가져오기
                val grouped = fetchGroupedRecipes()
                val groupedIds = grouped.map { it.recipeId }.toSet()

                // 🔸 2. 구매한 레시피 중 그룹화되지 않은 것만 필터링
                val purchased = fetchPurchasedRecipes()
                    .filter { it.recipeId !in groupedIds }

                // 🔸 3. 합치기
                val combined = grouped + purchased
                _galleryItems.value = combined

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
//    private suspend fun fetchGalleryItems(): List<GalleryItem> = coroutineScope {
//        val userId = UserManager.getUser()?.uid ?: return@coroutineScope emptyList()
//
//        try {
//            //  1. 그룹화된 레시피: user/{uid}/recipe_storage
//            val groupedSnapshot = db.collection("user")
//                .document(userId)
//                .collection("recipe_storage")
//                .get()
//                .await()
//
//            val groupedRecipeIds = groupedSnapshot.documents.map { it.id }.toSet()
//            val groupedGalleryItemsDeferred = groupedSnapshot.documents.map { doc ->
//                async {
//                    val recipeId = doc.id
//                    val groupId = doc.getString("groupId") ?: ""
//                    val groupName = doc.getString("groupName") ?: ""
//                    val name = doc.getString("name") ?: ""
//
//                    //  추가 정보 (이미지) Firestore에서 가져오기
//                    val recipeDoc = db.collection("recipe").document(recipeId).get().await()
//                    val recipe = recipeDoc.toObject(RecipeItem::class.java)
//
//                    GalleryItem(
//                        recipeId = recipeId,
//                        name = name,
//                        groupId = groupId,
//                        groupName = groupName,
//                        image = recipe?.imageResId,
//                        creationTimestamp = recipe?.date?.toDate()?.time ?: System.currentTimeMillis()
//                    )
//                }
//            }
//
//            //  2. 구매했지만 그룹되지 않은 레시피
//            val purchasedSnapshot = db.collection("user")
//                .document(userId)
//                .collection("purchased")
//                .whereEqualTo("purchased", true)
//                .get()
//                .await()
//
//            val ungroupedRecipeIds = purchasedSnapshot.documents
//                .map { it.id }
//                .filterNot { groupedRecipeIds.contains(it) }
//
//            val ungroupedGalleryItemsDeferred = ungroupedRecipeIds.map { recipeId ->
//                async {
//                    val recipeDoc = db.collection("recipe").document(recipeId).get().await()
//                    val recipe = recipeDoc.toObject(RecipeItem::class.java)
//                    recipe?.let {
//                        GalleryItem(
//                            recipeId = recipeId,
//                            name = it.name,
//                            groupId = "",
//                            groupName = "",
//                            image = it.imageResId,
//                            creationTimestamp = it.date.toDate().time
//                        )
//                    }
//                }
//            }
//
//            //  병합 결과
//            val groupedItems = groupedGalleryItemsDeferred.awaitAll().filterNotNull()
//            val ungroupedItems = ungroupedGalleryItemsDeferred.awaitAll().filterNotNull()
//
//            return@coroutineScope groupedItems + ungroupedItems
//        } catch (e: Exception) {
//            Log.e("GalleryFetch", "갤러리 항목 전체 불러오기 실패", e)
//            return@coroutineScope emptyList()
//        }
//    }



    private suspend fun fetchPurchasedRecipes(): List<GalleryItem> = coroutineScope {
        val userId = UserManager.getUser()?.uid ?: return@coroutineScope emptyList()

        try {
            val snapshot = db.collection("user")
                .document(userId)
                .collection("purchased")
                .whereEqualTo("purchased", true)
                .get()
                .await()

            val deferredItems = snapshot.documents.mapNotNull { doc ->
                val recipeId = doc.id
                async {
                    try {
                        val recipeDoc = db.collection("recipe")
                            .document(recipeId)
                            .get()
                            .await()

                        val recipe = recipeDoc.toObject(RecipeItem::class.java)
                        recipe?.let {
                            GalleryItem(
                                recipeId = recipeId,
                                name = it.name,
                                image = it.imageResId,
                                creationTimestamp = it.date.toDate().time,
                                groupId = "",
                                groupName = ""
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("GalleryFetch", "레시피 불러오기 실패: $recipeId", e)
                        null
                    }
                }
            }

            deferredItems.awaitAll().filterNotNull()

        } catch (e: Exception) {
            Log.e("GalleryFetch", "구매한 레시피 불러오기 실패", e)
            emptyList()
        }
    }


    private suspend fun fetchGroupedRecipes(): List<GalleryItem> = coroutineScope {
        val userId = UserManager.getUser()?.uid ?: return@coroutineScope emptyList()

        try {
            val snapshot = db.collection("user")
                .document(userId)
                .collection("recipe_storage")
                .get()
                .await()

            val deferredItems = snapshot.documents.mapNotNull { doc ->
                val recipeId = doc.id
                val groupId = doc.getString("groupId") ?: ""
                val groupName = doc.getString("groupName") ?: ""

                if (groupId.isBlank()) return@mapNotNull null

                async {
                    try {
                        val recipeDoc = db.collection("recipe")
                            .document(recipeId)
                            .get()
                            .await()

                        val recipe = recipeDoc.toObject(RecipeItem::class.java)
                        recipe?.let {
                            GalleryItem(
                                recipeId = recipeId,
                                name = it.name,
                                image = it.imageResId,
                                creationTimestamp = it.date.toDate().time,
                                groupId = groupId,
                                groupName = groupName
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("GalleryFetch", "그룹 레시피 불러오기 실패: $recipeId", e)
                        null
                    }
                }
            }

            deferredItems.awaitAll().filterNotNull()

        } catch (e: Exception) {
            Log.e("GalleryFetch", "그룹 레시피 전체 불러오기 실패", e)
            emptyList()
        }
    }


    /**
     * 특정 레시피의 그룹 ID를 Firestore 및 StateFlow에 반영
     */
    fun updateItemGroup(recipeId: String, newGroupId: String, newGroupName: String? = null) {
        viewModelScope.launch {
            try {
                val userId = UserManager.getUser()?.uid ?: return@launch

                // 1. recipe/{recipeId} 문서에서 name 필드 가져오기
                val recipeSnapshot = db.collection("recipe")
                    .document(recipeId)
                    .get()
                    .await()

                if (!recipeSnapshot.exists()) {
                    Log.e("FirestoreUpdate", " 레시피 문서 없음: $recipeId")
                    return@launch
                }

                val recipeName = recipeSnapshot.getString("name") ?: "Unknown"

                // 2. 저장할 필드 구성
                val updates = mutableMapOf<String, Any>(
                    "groupId" to newGroupId,
                    "name" to recipeName
                )
                newGroupName?.let { updates["groupName"] = it }

                // 3. users/{uid}/recipe_storage/{recipeId}에 merge로 저장
                db.collection("user")
                    .document(userId)
                    .collection("recipe_storage")
                    .document(recipeId)
                    .set(updates, SetOptions.merge())
                    .await()

                // 4. local 상태 갱신
                _galleryItems.value = _galleryItems.value.map {
                    if (it.recipeId == recipeId)
                        it.copy(groupId = newGroupId, groupName = newGroupName ?: it.groupName)
                    else it
                }

                Log.d("FirestoreUpdate", " 그룹 및 이름 업데이트 완료: $recipeId → $newGroupId ($recipeName)")

            } catch (e: Exception) {
                Log.e("FirestoreUpdate", " 그룹 정보 업데이트 실패", e)
            }
        }
    }





    // 그룹 생성
    fun createGroup(item1: GalleryItem, item2: GalleryItem) {
        val groupId = when {
            !item1.groupId.isNullOrEmpty() -> item1.groupId
            !item2.groupId.isNullOrEmpty() -> item2.groupId
            else -> UUID.randomUUID().toString()
        }

        val groupName = generateNextGroupName()

        // 중복 drop 방지
        if (item1.groupId == groupId && item2.groupId == groupId) return

        Log.d("GroupAction", " 그룹 생성: ${item1.recipeId} + ${item2.recipeId} → groupId = $groupId, name = $groupName")

        updateItemGroup(item1.recipeId, groupId, groupName)
        updateItemGroup(item2.recipeId, groupId, groupName)
    }

    // 기존 그룹에 레시피 추가

    fun addToGroup(targetGroupId: String, item: GalleryItem) {
        // 이미 같은 그룹이면 무시
        if (item.groupId == targetGroupId) return

        // 해당 그룹의 대표 항목 기준으로 그룹 이름 가져오기
        val groupName = galleryItems.value
            .filter { it.groupId == targetGroupId }
            .maxByOrNull { it.creationTimestamp ?: 0L }  // 가장 최근 생성된 항목
            ?.groupName ?: "Unnamed"

        Log.d("GroupAction", "그룹에 추가: ${item.recipeId} → groupId = $targetGroupId, name = $groupName")

        // Firestore 및 local state에 업데이트
        updateItemGroup(item.recipeId, targetGroupId, groupName)
    }


    // 그룹 삭제
    fun ungroup(groupId: String) {
        viewModelScope.launch {
            try {
                val userId = UserManager.getUser()?.uid ?: return@launch
                val itemsToUngroup = _galleryItems.value.filter { it.groupId == groupId }

                itemsToUngroup.forEach { item ->
                    db.collection("user")
                        .document(userId)
                        .collection("recipe_storage")
                        .document(item.recipeId)
                        .delete()
                        .await()
                }

                // UI 상태 반영: groupId/groupName 제거
                _galleryItems.value = _galleryItems.value.map {
                    if (it.groupId == groupId) it.copy(groupId = "", groupName = "")
                    else it
                }

                Log.d("GroupAction", " 그룹 해제 및 문서 삭제 완료: $groupId → ${itemsToUngroup.size}개 삭제됨")

            } catch (e: Exception) {
                Log.e("GroupAction", " 그룹 해제 실패", e)
            }
        }
    }


    // 그룹 이름 자동 할당
    private fun generateNextGroupName(): String {
        val existingNames = galleryItems.value
            .filter { it.groupId.isNotBlank() }
            .mapNotNull { it.groupName }
        val regex = Regex("^Group(\\d+)$")
        val groupNumbers = existingNames.mapNotNull {
            regex.matchEntire(it)?.groupValues?.get(1)?.toIntOrNull()
        }
        val nextNumber = if (groupNumbers.isEmpty()) 1 else groupNumbers.max()!! + 1
        return "Group$nextNumber"
    }
    // 그룹 이름 변경
    fun renameGroup(groupId: String, newName: String) {
        viewModelScope.launch {
            try {
                val userId = UserManager.getUser()?.uid ?: return@launch
                val itemsToUpdate = _galleryItems.value.filter { it.groupId == groupId }

                itemsToUpdate.forEach { item ->
                    db.collection("user")
                        .document(userId)
                        .collection("recipe_storage")
                        .document(item.recipeId)
                        .update("groupName", newName)
                }

                _galleryItems.value = _galleryItems.value.map {
                    if (it.groupId == groupId) it.copy(groupName = newName)
                    else it
                }

                Log.d("RenameGroup", " 그룹 이름 변경 완료: $groupId → $newName")

            } catch (e: Exception) {
                Log.e("RenameGroup", " 그룹 이름 변경 실패", e)
            }
        }
    }


    private fun updateItemGroupName(recipeId: String, newName: String) {
        // Firestore 등에서 해당 레시피의 groupName 필드를 newName으로 수정
        db.collection("recipe")
            .document(recipeId)
            .update("groupName", newName)
    }





}
