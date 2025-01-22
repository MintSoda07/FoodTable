package com.bcu.foodtable.useful

import android.net.Uri
import android.util.Log
import android.widget.ImageView
import com.bcu.foodtable.R
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

// 파이어스토어 관련 동작을 한번에 처리하기 위해 작성된 함수
// (이론상) 업로드 다운로드, 삭제, 이미지 불러오기 모두 처리 할 수 있음.

object FireStoreHelper {
    private fun getFirestoreInstance(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    private val storage = FirebaseStorage.getInstance()

    // 이미지 업로드
    fun uploadImage(
        imageUri: Uri,
        imageName: String,
        collectionName: String,
        folderName: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val storageRef = storage.reference.child("$folderName/$imageName")
        val firestore = getFirestoreInstance()

        storageRef.putFile(imageUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    val imageUrl = uri.toString()

                    // Firestore에 URL 저장
                    val imageData = hashMapOf("image" to imageUrl)
                    firestore.collection(collectionName).document(imageName)
                        .set(imageData)
                        .addOnSuccessListener {
                            onSuccess(imageUrl)
                        }
                        .addOnFailureListener { exception ->
                            onFailure(exception)
                        }
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
        // Firebase Storage 인스턴스 가져오기
        val storage = FirebaseStorage.getInstance()
        val storageRef: StorageReference = storage.reference.child(storagePath)

        // 다운로드 URL 가져오기
        storageRef.downloadUrl
            .addOnSuccessListener { uri ->
                // Glide를 사용하여 ImageView에 로드
                Log.d("FirebaseStorage", "Image downloading.. $uri")

                if (imageView.context != null) {
                    Glide.with(imageView.context)
                        .load(uri) // 다운로드 URL을 Glide에 전달
                        .placeholder(R.drawable.baseline_menu_book_24) // 로딩 중 표시할 이미지
                        .error(R.drawable.dish_icon) // 실패 시 표시할 이미지
                        .into(imageView) // ImageView에 로드
                } else {
                    Log.e("FirebaseStorage", "Context is null, cannot load image.")
                }
            }
            .addOnFailureListener { exception ->
                // 다운로드 실패 처리
                Log.e("FirebaseStorage", "storageRef is : ${storage.reference.child(storagePath)} \n ${storagePath}\nImage download failed: ${exception.message}", exception)
            }
    }
    // 그 외의 경우에는 이거 사용
    fun loadImageFromUrl(imageUrl: String, imageView: ImageView) {
        if (imageView.context != null) {
            Glide.with(imageView.context)
                .load(imageUrl) // 매개변수로 전달받은 URL을 그대로 사용
                .centerCrop()
                .placeholder(R.drawable.baseline_menu_book_24) // 로딩 중 표시할 이미지
                .error(R.drawable.dish_icon) // 실패 시 표시할 이미지
                .into(imageView) // ImageView에 로드
        } else {
            Log.e("FirebaseStorage", "Context is null, cannot load image.")
        }
    }
}