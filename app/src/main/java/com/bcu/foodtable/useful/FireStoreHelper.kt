package com.bcu.foodtable.useful

import android.net.Uri
import android.util.Log
import android.widget.ImageView
import com.bcu.foodtable.R
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.tasks.await

object FireStoreHelper {
    private fun getFirestoreInstance(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // 이미지 업로드
    fun uploadImage(
        imageUri: Uri,
        imageName: String,
        folderName: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val storageRef = storage.reference.child("$folderName/$imageName")

        storageRef.putFile(imageUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    val imageUrl = uri.toString()
                    onSuccess(imageUrl)

                }.addOnFailureListener { exception ->
                    onFailure(exception)
                }
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    // Firestore에서 이미지 URL 불러오기
    fun getImageUrl(
        imageName: String,
        collectionName: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val firestore = getFirestoreInstance()
        firestore.collection(collectionName).document(imageName)
            .get()
            .addOnSuccessListener { document ->
                val imageUrl = document.getString("url")
                if (imageUrl != null) {
                    onSuccess(imageUrl)
                } else {
                    onFailure(Exception("No image URL found for the document"))
                }
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    // Firestore 및 Storage에서 이미지 삭제
    fun deleteImage(
        imageName: String,
        collectionName: String,
        folderName: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        // Firestore에서 문서 삭제
        val firestore = getFirestoreInstance()

        firestore.collection(collectionName).document(imageName)
            .delete()
            .addOnSuccessListener {
                // Storage에서 파일 삭제
                val storageRef = storage.reference.child("$folderName/$imageName")
                storageRef.delete()
                    .addOnSuccessListener {
                        onSuccess()
                    }
                    .addOnFailureListener { exception ->
                        onFailure(exception)
                    }
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    // gs// 같은 Firebase 내부 경로가 url일 경우 사용
    fun loadImageFromStorageFromGS(storagePath: String, imageView: ImageView) {
        val storageRef: StorageReference = storage.reference.child(storagePath)

        storageRef.downloadUrl
            .addOnSuccessListener { uri ->
                Log.d("FirebaseStorage", "Image downloading.. $uri")

                if (imageView.context != null) {
                    Glide.with(imageView.context)
                        .load(uri)
                        .placeholder(R.drawable.baseline_menu_book_24)
                        .error(R.drawable.dish_icon)
                        .into(imageView)
                } else {
                    Log.e("FirebaseStorage", "Context is null, cannot load image.")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseStorage", "storageRef is : ${storage.reference.child(storagePath)} \n ${storagePath}\nImage download failed: ${exception.message}", exception)
            }
    }

    // 일반 URL로 이미지 불러오기
    fun loadImageFromUrl(imageUrl: String, imageView: ImageView) {
        if (imageView.context != null  && imageView.tag != imageUrl) {
            Glide.with(imageView.context)
                .load(imageUrl)
                .centerCrop()
                .placeholder(R.drawable.baseline_menu_book_24)
                .override(imageView.width, imageView.height)
                .error(R.drawable.dish_icon)
                .into(imageView)
            imageView.tag = imageUrl
        } else {
            Log.e("FirebaseStorage", "Context is null, cannot load image.")
        }
    }

    // 카테고리 데이터 추가 (배치)
    fun addCategoriesToFirestore() {
        val C_categories = mapOf(
            "C_food_types" to mapOf("list" to listOf("한식", "양식", "중식", "일식", "퓨전식","채식","패스트푸드","건강식","아메리카음식","아프리카음식","디저트")),
            "C_cooking_methods" to mapOf("list" to listOf("볶음", "튀김", "찜", "구이", "국/찌개","조림")),
            "C_ingredients" to mapOf("list" to listOf("소고기", "돼지고기", "닭고기", "오리고기", "새우","연아","게","오징어","감자","고구마","양파","당근","계란","두부","버섯","치즈"))
        )

        val batch = db.batch()
        C_categories.forEach { (key, value) ->
            val docRef = db.collection("C_categories").document(key)
            batch.set(docRef, value)
        }

        batch.commit()
            .addOnSuccessListener { Log.d("Firestore", "카테고리 데이터 추가 완료") }
            .addOnFailureListener { e -> Log.e("Firestore", "카테고리 추가 실패", e) }
    }

    // Compose용 채널 조회
    suspend fun getChannel(channelName: String): Channel? {
        return try {
            val doc = db.collection("channels").document(channelName).get().await()
            doc.toObject<Channel>()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getRecipesForChannel(channelName: String): List<RecipeItem> {
        return try {
            db.collection("recipes")
                .whereEqualTo("contained_channel", channelName)
                .get().await()
                .documents.mapNotNull { it.toObject<RecipeItem>() }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun isUserSubscribed(channelName: String, userId: String): Boolean {
        return try {
            val snapshot = db.collection("subscriptions")
                .document(channelName)
                .collection("users")
                .document(userId)
                .get()
                .await()
            snapshot.exists()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun subscribeChannel(channelName: String, userId: String) {
        try {
            db.collection("subscriptions")
                .document(channelName)
                .collection("users")
                .document(userId)
                .set(mapOf("subscribed" to true))
                .await()

            val channelRef = db.collection("channels").document(channelName)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(channelRef)
                val current = snapshot.getLong("subscribers") ?: 0L
                transaction.update(channelRef, "subscribers", current + 1)
            }.await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun unsubscribeChannel(channelName: String, userId: String) {
        try {
            db.collection("subscriptions")
                .document(channelName)
                .collection("users")
                .document(userId)
                .delete()
                .await()

            val channelRef = db.collection("channels").document(channelName)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(channelRef)
                val current = snapshot.getLong("subscribers") ?: 1L
                transaction.update(channelRef, "subscribers", (current - 1).coerceAtLeast(0))
            }.await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}