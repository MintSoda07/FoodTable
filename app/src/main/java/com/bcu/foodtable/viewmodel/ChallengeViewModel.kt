package com.bcu.foodtable.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bcu.foodtable.model.Challenge
import com.bcu.foodtable.repository.ChallengeRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChallengeViewModel : ViewModel() {
    private val firestore = Firebase.firestore
    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val _challenges = MutableStateFlow<List<Challenge>>(emptyList())
    val challenges: StateFlow<List<Challenge>> = _challenges

    val loading = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)
    val userSalt = MutableStateFlow(0)

    init {
        fetchChallenges()
        fetchUserSalt()
    }

    fun fetchChallenges() {
        loading.value = true
        firestore.collection("challenges")
            .get()
            .addOnSuccessListener { result ->
                _challenges.value = result.documents.mapNotNull {
                    it.toObject(Challenge::class.java)?.copy(id = it.id)
                }
                loading.value = false
            }
            .addOnFailureListener {
                error.value = it.message
                loading.value = false
            }
    }

    fun fetchUserSalt() {
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { doc ->
                val salt = doc.getLong("salt") ?: 0
                userSalt.value = salt.toInt()
            }
    }

    fun updateProgress(id: String, value: Int) {
        val challenge = _challenges.value.find { it.id == id } ?: return

        firestore.collection("challenges").document(id)
            .update("progress", value)
            .addOnSuccessListener {
                // ✅ 보상 지급 처리
                if (value >= challenge.targetValue && !challenge.isCompleted) {
                    giveRewardToUser(challenge.reward)
                    firestore.collection("challenges").document(id).update("isCompleted", true)
                }
                fetchChallenges()
            }
            .addOnFailureListener {
                Log.e("Challenge", "진행도 업데이트 실패", it)
            }
    }

    fun startChallenge(id: String) {
        firestore.collection("challenges").document(id)
            .update("progress", 1)
            .addOnSuccessListener {
                fetchChallenges()
            }
            .addOnFailureListener {
                Log.e("Challenge", "도전 시작 실패", it)
            }
    }

    private fun giveRewardToUser(reward: Int) {
        val userRef = firestore.collection("users").document(userId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val currentSalt = snapshot.getLong("salt") ?: 0
            transaction.update(userRef, "salt", currentSalt + reward)
        }.addOnSuccessListener {
            fetchUserSalt()
        }.addOnFailureListener {
            Log.e("Challenge", "소금 보상 지급 실패", it)
        }
    }
}