package com.bcu.foodtable.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bcu.foodtable.model.Challenge
import com.bcu.foodtable.model.ChallengeType
import com.bcu.foodtable.useful.UserManager
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.bcu.foodtable.util.ChallengeUtils.generateRandomChallenges

class ChallengeViewModel : ViewModel() {
    private val firestore = Firebase.firestore
    private val user = UserManager.getUser()!!
    private val uid = user.uid

    private val _challenges = MutableStateFlow<List<Challenge>>(emptyList())
    val challenges: StateFlow<List<Challenge>> = _challenges

    val loading = MutableStateFlow(true)
    val error = MutableStateFlow<String?>(null)
    val userSalt = MutableStateFlow(user.point)

    init {
        fetchChallenges()
    }

    fun fetchChallenges() {
        viewModelScope.launch {
            loading.value = true
            try {
                val ref = firestore.collection("user").document(uid).collection("ActiveChallenges")
                val snapshot = ref.get().await()

                val result = if (snapshot.isEmpty) {
                    val daily = generateRandomChallenges(ChallengeType.DAILY, 3)
                    val weekly = generateRandomChallenges(ChallengeType.WEEKLY, 2)
                    val all = daily + weekly

                    all.forEach { challenge ->
                        ref.document(challenge.id).set(challenge)
                    }
                    all
                } else {
                    snapshot.documents.mapNotNull { it.toObject(Challenge::class.java) }
                }

                _challenges.value = result
            } catch (e: Exception) {
                error.value = e.localizedMessage
                Log.e("ChallengeViewModel", "챌린지 불러오기 실패", e)
            } finally {
                loading.value = false
            }
        }
    }

    fun updateProgress(id: String, value: Int) {
        viewModelScope.launch {
            try {
                val updated = _challenges.value.map {
                    if (it.id == id) it.copy(progress = value, isCompleted = true) else it
                }
                _challenges.value = updated

                val reward = updated.find { it.id == id }?.reward ?: 0

                // 진행도 및 완료 처리
                val challengeRef = firestore.collection("user").document(uid)
                    .collection("ActiveChallenges").document(id)

                challengeRef.update(
                    mapOf(
                        "progress" to value,
                        "isCompleted" to true
                    )
                )

                // 유저 소금 보상
                updateUserSalt(reward)

                // 히스토리 저장
                val historyRef = firestore.collection("user").document(uid)
                    .collection("ChallengeHistory").document(id)

                historyRef.set(updated.find { it.id == id }!!)
            } catch (e: Exception) {
                Log.e("ChallengeViewModel", "진행도 업데이트 실패", e)
            }
        }
    }

    fun startChallenge(id: String) {
        viewModelScope.launch {
            try {
                val challengeRef = firestore.collection("user")
                    .document(uid)
                    .collection("ActiveChallenges")
                    .document(id)

                challengeRef.update("progress", 1)
                fetchChallenges()
            } catch (e: Exception) {
                Log.e("ChallengeViewModel", "챌린지 시작 실패", e)
            }
        }
    }

    private fun updateUserSalt(amount: Int) {
        viewModelScope.launch {
            val userRef = firestore.collection("user").document(uid)
            try {
                firestore.runTransaction { tx ->
                    val snapshot = tx.get(userRef)
                    val currentSalt = snapshot.getLong("point") ?: 0
                    val newSalt = currentSalt + amount
                    tx.update(userRef, "point", newSalt)
                    userSalt.value = newSalt.toInt()

                    val updatedUser = user.copy(point = newSalt.toInt())
                    UserManager.setUserByDatatype(updatedUser)
                }.await()
            } catch (e: Exception) {
                Log.e("ChallengeViewModel", "소금 지급 실패", e)
            }
        }
    }

}
