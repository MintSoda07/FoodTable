package com.bcu.foodtable.ui

import android.util.Log
import androidx.compose.runtime.*
import com.bcu.foodtable.model.Challenge
import com.bcu.foodtable.model.ChallengeType
import com.bcu.foodtable.useful.UserManager
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.util.*
import com.bcu.foodtable.util.ChallengeUtils.generateRandomChallenges

@Composable
fun ChallengeFirebaseManager() {
    val uid = UserManager.getUser()!!.uid
    val db = Firebase.firestore
    val activeRef = db.collection("user").document(uid).collection("ActiveChallenges")

    var challenges by remember { mutableStateOf<List<Challenge>>(emptyList()) }
    var salt by remember { mutableStateOf(UserManager.getUser()!!.point) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val snapshot = activeRef.get().await()
            if (snapshot.isEmpty) {
                val daily = generateRandomChallenges(ChallengeType.DAILY, 3)
                val weekly = generateRandomChallenges(ChallengeType.WEEKLY, 2)
                val all = daily + weekly

                all.forEach { challenge ->
                    activeRef.document(challenge.id).set(challenge)
                }
                challenges = all
            } else {
                challenges = snapshot.documents.mapNotNull { it.toObject(Challenge::class.java) }
            }
        } catch (e: Exception) {
            Log.e("ChallengePreview", "Firestore 오류: ${e.localizedMessage}")
        } finally {
            isLoading = false
        }
    }

    if (!isLoading) {
        ChallengeScreenContent(
            challenges = challenges,
            salt = salt,
            onProgressUpdate = { id, value ->
                val updated = challenges.map {
                    if (it.id == id) it.copy(progress = value, isCompleted = true) else it
                }
                challenges = updated
                val reward = updated.find { it.id == id }?.reward ?: 0
                salt += reward
                val user = UserManager.getUser()!!.copy(point = salt)
                UserManager.setUserByDatatype(user)
                Firebase.firestore.collection("user").document(user.uid).update("point", salt)
                db.collection("user").document(uid).collection("ChallengeHistory")
                    .document(id).set(updated.find { it.id == id }!!)
            },
            onStartChallenge = { id ->
                Log.d("Challenge", "챌린지 시작: $id")
            }
        )
    }
}

suspend fun loadOrCreateChallenges(uid: String): List<Challenge> {
    val db = Firebase.firestore
    val activeRef = db.collection("user").document(uid).collection("ActiveChallenges")

    return try {
        val snapshot = activeRef.get().await()
        if (snapshot.isEmpty) {
            val daily = generateRandomChallenges(ChallengeType.DAILY, 3)
            val weekly = generateRandomChallenges(ChallengeType.WEEKLY, 2)
            val all = daily + weekly

            all.forEach { challenge ->
                activeRef.document(challenge.id).set(challenge)
            }
            all
        } else {
            snapshot.documents.mapNotNull { it.toObject(Challenge::class.java) }
        }
    } catch (e: Exception) {
        Log.e("ChallengeInit", "Firestore 오류: ${e.localizedMessage}")
        emptyList()
    }
}
