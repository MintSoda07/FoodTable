package com.bcu.foodtable.useful

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

// FirebaseHelper 오브젝트는 파이어베이스 관력 작업을 처리해 줌.
// 다양한 작업을 처리할 수 있으므로 사용 권장. (조금 복잡하긴 함)
object FirebaseHelper {

    private fun getFirestoreInstance(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }


    // suspend가 붙은 이유는 조금 시간이 걸리는 네트워크 요청이므로, 비동기로 처리하기 위함.
    // 특정 컬렉션에서 모든 문서를 가져오는 함수
    suspend fun <T> getAllDocuments(
        collectionPath: String,
        documentClass: Class<T>
    ): List<T> {
        val db = getFirestoreInstance()
        return try {
            val querySnapshot = db.collection(collectionPath).get().await()
            querySnapshot.documents.mapNotNull { it.toObject(documentClass) }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList() // 에러 시 빈 리스트 반환
        }
    }

    // 특정 문서를 ID로 가져오는 함수
    suspend fun <T> getDocumentById(
        collectionPath: String,
        documentId: String,
        documentClass: Class<T>
    ): T? {
        val db = getFirestoreInstance()
        return try {
            val documentSnapshot = db.collection(collectionPath).document(documentId).get().await()
            documentSnapshot.toObject(documentClass)
        } catch (e: Exception) {
            e.printStackTrace()
            null // 에러 시 null 반환
        }
    }

    // Firestore에 데이터 추가 (새 문서 생성)
    suspend fun addDocument(
        collectionPath: String,
        data: Any
    ): Boolean {
        val db = getFirestoreInstance()
        return try {
            db.collection(collectionPath).add(data).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false // 에러 시 false 반환
        }
    }

    // Firestore 문서 업데이트
    suspend fun updateDocument(
        collectionPath: String,
        documentId: String,
        updates: Map<String, Any>
    ): Boolean {
        val db = getFirestoreInstance()
        return try {
            db.collection(collectionPath).document(documentId).update(updates).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Firestore 문서 삭제
    suspend fun deleteDocument(
        collectionPath: String,
        documentId: String
    ): Boolean {
        val db = getFirestoreInstance()
        return try {
            db.collection(collectionPath).document(documentId).delete().await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // 조건에 따라 데이터를 가져오는 함수
    suspend fun <T> getDocumentsByField(
        collectionPath: String,
        fieldName: String,
        fieldValue: Any,
        documentClass: Class<T>
    ): List<T> {
        val db = getFirestoreInstance()
        return try {
            val querySnapshot = db.collection(collectionPath)
                .whereEqualTo(fieldName, fieldValue)
                .get()
                .await()

            querySnapshot.documents.mapNotNull { it.toObject(documentClass) }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList() // 에러 시 빈 리스트 반환
        }
    }

    // 복합 조건으로 데이터를 가져오는 함수
    suspend fun <T> getDocumentsByMultipleFields(
        collectionPath: String,
        conditions: List<Pair<String, Any>>,
        documentClass: Class<T>
    ): List<T> {
        val db = getFirestoreInstance()
        return try {
            // query를 Query 타입으로 선언
            var query: Query = db.collection(collectionPath)
            conditions.forEach { (field, value) ->
                query = query.whereEqualTo(field, value)
            }

            val querySnapshot = query.get().await()
            querySnapshot.documents.mapNotNull { it.toObject(documentClass) }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    suspend fun updateFieldById(collectionPath: String, documentId: String, fieldName: String, newValue: Any) {
        val updateMap = mapOf(fieldName to newValue)
        val isUpdated = updateDocument(collectionPath, documentId, updateMap)

        if (isUpdated) {
            println("Document with ID $documentId updated successfully!")
        } else {
            println("Failed to update document with ID $documentId.")
        }
    }

    suspend fun getApiKeyInfo(documentId: String): ApiKey? {
        return try {
            val db = getFirestoreInstance()
            val documentSnapshot = db.collection("API_KEY").document(documentId).get().await()
            documentSnapshot.toObject(ApiKey::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
//    사용예시 : 데이터를 받아서 사용하기
//    CoroutineScope(Dispatchers.IO).launch {
//        val apiKeyInfo = getApiKeyInfo("ABCD1234")  // ApiKeyInfo 객체로 반환됨
//
//        // 객체가 null이 아닌지 확인한 후 사용
//        apiKeyInfo?.let { info ->
//            println("API Name: ${info.API_NAME}")
//            println("API Key: ${info.API_KEY}")
//        }
//    }
}