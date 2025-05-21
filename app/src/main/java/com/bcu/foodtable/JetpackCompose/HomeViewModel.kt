package com.bcu.foodtable.JetpackCompose

import com.bcu.foodtable.useful.RecipeItem
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bcu.foodtable.useful.Channel

import com.bcu.foodtable.useful.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class HomeViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _popularChannels = MutableStateFlow<List<Channel>>(emptyList())
    val popularChannels: StateFlow<List<Channel>> = _popularChannels

    fun loadPopularChannels(limit: Long = 10) {
        firestore.collection("channel")
            .orderBy("subscribers", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .addOnSuccessListener { result ->
                val channels = result.mapNotNull { it.toObject(Channel::class.java) }
                _popularChannels.value = channels
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "채널 불러오기 실패", e)
            }
    }
    // ✅ 레시피 상태
    private val _recipes = MutableStateFlow<List<RecipeItem>>(emptyList())
    val recipes: StateFlow<List<RecipeItem>> = _recipes

    // ✅ 사용자 상태
    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

    // ✅ 레시피 불러오기
    fun loadRecipes() {
        viewModelScope.launch {
            try {
                Log.d("HomeViewModel", "Firestore에서 recipe 컬렉션 조회 시작")

                val snapshot = db.collection("recipe").get().await()
                val list = snapshot.documents.mapNotNull {
                    val recipe = it.toObject(RecipeItem::class.java)
                    Log.d("HomeViewModel", "Loaded recipe: ${recipe?.name} - ${recipe?.imageResId}")
                    recipe
                }

                Log.d("HomeViewModel", "총 불러온 레시피 수: ${list.size}")
                _recipes.value = list

            } catch (e: Exception) {
                Log.e("HomeViewModel", "레시피 불러오기 실패: ${e.message}")
                _recipes.value = emptyList()
            }
        }
    }

    // ✅ 사용자 정보 불러오기
    fun loadUserInfo() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                Log.d("HomeViewModel", "Firestore에서 사용자 정보 조회 시작")

                val snapshot = db.collection("user").document(uid).get().await()
                val userData = snapshot.toObject(User::class.java)

                if (userData != null) {
                    Log.d("HomeViewModel", "유저 정보 로드 성공: ${userData.name}, 소금: ${userData.point}")
                    _user.value = userData
                } else {
                    Log.e("HomeViewModel", "유저 정보 변환 실패")
                }

            } catch (e: Exception) {
                Log.e("HomeViewModel", "유저 정보 로드 실패: ${e.message}")
            }
        }
    }
}
