// --- import 구문 ---
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bcu.foodtable.useful.Channel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// --- ChannelData는 별도 파일/공유 구조로 필요 ---
//   data class ChannelData(
//     val documentId: String,
//     val name: String,
//     val description: String,
//     val imageUri: String?,
//     val backgroundUri: String?
// )

class ChannelEditViewModel : ViewModel() {
    val channelName = MutableStateFlow("")
    val channelDescription = MutableStateFlow("")
    val selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedBackgroundUri = MutableStateFlow<Uri?>(null)
    val isUploading = MutableStateFlow(false)

    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private var _channelDocId: String? = null
    private var _originalChannelName: String? = null

    /** 초기 데이터 세팅 (수정 진입시 1회) */
    fun setInitialData(channel: Channel) {
        channelName.value = channel.name
        channelDescription.value = channel.description
        selectedImageUri.value = if (channel.imageResId.isNotBlank()) Uri.parse(channel.imageResId) else null
        selectedBackgroundUri.value = if (channel.backgroundResId.isNotBlank()) Uri.parse(channel.backgroundResId) else null
        _channelDocId = channel.documentId
        _originalChannelName = channel.name
    }

    fun setImageUri(uri: Uri?) { selectedImageUri.value = uri }
    fun setBackgroundUri(uri: Uri?) { selectedBackgroundUri.value = uri }

    /** 채널 정보 업데이트 (이미지 변경시 업로드 포함) */
    fun updateChannel(context: Context, onSuccess: () -> Unit) {
        val docId = _channelDocId ?: return
        val updates = mutableMapOf<String, Any>(
            "name" to channelName.value,
            "description" to channelDescription.value
        )
        isUploading.value = true

        viewModelScope.launch {
            try {
                // 대표 이미지 업로드
                if (selectedImageUri.value != null && !selectedImageUri.value.toString().startsWith("http")) {
                    val imgRef = storage.reference.child("channel_images/${System.currentTimeMillis()}.jpg")
                    imgRef.putFile(selectedImageUri.value!!).await()
                    val url = imgRef.downloadUrl.await().toString()
                    updates["imageResId"] = url
                }
                // 배경 이미지 업로드
                if (selectedBackgroundUri.value != null && !selectedBackgroundUri.value.toString().startsWith("http")) {
                    val bgRef = storage.reference.child("channel_bg/${System.currentTimeMillis()}.jpg")
                    bgRef.putFile(selectedBackgroundUri.value!!).await()
                    val url = bgRef.downloadUrl.await().toString()
                    updates["backgroundResId"] = url
                }

                firestore.collection("channel").document(docId)
                    .update(updates)
                    .addOnSuccessListener {
                        Toast.makeText(context, "채널이 수정되었습니다.", Toast.LENGTH_SHORT).show()
                        isUploading.value = false
                        onSuccess()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "채널 수정 실패", Toast.LENGTH_SHORT).show()
                        isUploading.value = false
                    }
            } catch (e: Exception) {
                Toast.makeText(context, "이미지 업로드 실패: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                isUploading.value = false
            }
        }
    }

    /** 채널 삭제 + 해당 채널 관련 모든 레시피 삭제 */
    fun deleteChannelAndRecipes(context: Context, onSuccess: () -> Unit) {
        val docId = _channelDocId ?: return
        val channelName = _originalChannelName ?: return
        isUploading.value = true

        firestore.collection("channel").document(docId).delete()
            .addOnSuccessListener {
                // 2. 해당 채널 포함 레시피도 삭제
                firestore.collection("recipe")
                    .whereEqualTo("contained_channel", channelName)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        val batch = firestore.batch()
                        querySnapshot.documents.forEach { batch.delete(it.reference) }
                        batch.commit().addOnSuccessListener {
                            Toast.makeText(context, "채널과 관련 레시피가 모두 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                            isUploading.value = false
                            onSuccess()
                        }.addOnFailureListener {
                            Toast.makeText(context, "레시피 삭제 중 오류 발생", Toast.LENGTH_SHORT).show()
                            isUploading.value = false
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "레시피 삭제 쿼리 실패", Toast.LENGTH_SHORT).show()
                        isUploading.value = false
                    }
            }
            .addOnFailureListener {
                Toast.makeText(context, "채널 삭제 실패", Toast.LENGTH_SHORT).show()
                isUploading.value = false
            }
    }
}
