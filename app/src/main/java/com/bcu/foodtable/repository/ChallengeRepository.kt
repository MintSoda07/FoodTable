package com.bcu.foodtable.repository


import com.bcu.foodtable.model.Challenge
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ChallengeRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val challengeCollection = firestore.collection("challenges")

    suspend fun getChallenges(): Flow<List<Challenge>> = flow {
        val snapshot = challengeCollection.get().await()
        val challenges = snapshot.toObjects(Challenge::class.java)
        emit(challenges)
    }

    suspend fun updateChallengeProgress(challengeId: String, progress: Int) {
        challengeCollection.document(challengeId)
            .update("progress", progress)
            .await()
    }

    suspend fun completeChallenge(challengeId: String) {
        challengeCollection.document(challengeId)
            .update("isCompleted", true)
            .await()
    }

    suspend fun createChallenge(challenge: Challenge) {
        val documentRef = challengeCollection.document()
        challengeCollection.document(documentRef.id)
            .set(challenge.copy(id = documentRef.id))
            .await()
    }
}
