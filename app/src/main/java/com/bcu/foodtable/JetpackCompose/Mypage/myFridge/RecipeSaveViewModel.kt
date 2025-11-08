package com.bcu.foodtable.JetpackCompose.Mypage.myFridge

import android.util.Log
import androidx.lifecycle.ViewModel
import com.bcu.foodtable.useful.Channel
import com.bcu.foodtable.useful.RecipeItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

class RecipeSaveViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val _myChannels = MutableStateFlow<List<Channel>>(emptyList())
    val myChannels: StateFlow<List<Channel>> = _myChannels

    private val _saveSuccess = MutableStateFlow<Boolean?>(null)
    val saveSuccess: StateFlow<Boolean?> = _saveSuccess

    fun loadMyChannels(userId: String) {
        db.collection("channel")
            .whereEqualTo("owner", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val channels = snapshot.documents.mapNotNull { doc ->
                    Log.i("AI ChatTest", "내 채널 로드: ${doc.id}")
                    doc.toObject(Channel::class.java)?.copy(documentId = doc.id)
                }
                _myChannels.value = channels
            }
            .addOnFailureListener { e ->
                Log.e("AI ChatTest", "loadMyChannels 실패: ${e.message}", e)
            }
    }

    /** A안: 저장 직전 정규화만 적용 (WriteScreen 포맷과 동일하게 맞춤) */
    suspend fun saveRecipeToChannel(
        recipe: RecipeItem,
        selectedChannel: Channel,
        userId: String
    ) {
        try {
            // 1) 이미지 URL 확정
            val finalImageUrl = resolveImageUrl(recipe.imageResId)

            // 2) 문서 ID 미리 생성
            val coll = db.collection("recipe")
            val doc = coll.document()

            // 3) WriteScreen 규격으로 정규화 + 문서 ID 주입
            val normalized = normalizeRecipeForWriteScreen(
                original = recipe.copy(
                    imageResId = finalImageUrl,
                    contained_channel = selectedChannel.name
                ),
                ownerUid = userId,
                newDocId = doc.id
            )

            // 4) 저장
            doc.set(normalized).await()

            // 5) 구매 플래그(내 보관함 마킹)
            db.collection("user")
                .document(userId)
                .collection("purchased")
                .document(doc.id)
                .set(mapOf("purchased" to true))
                .await()

            _saveSuccess.value = true
        } catch (e: Exception) {
            Log.e("RecipeSave", "saveRecipeToChannel 실패: ${e.message}", e)
            _saveSuccess.value = false
        }
    }

    fun resetSaveSuccess() {
        Log.i("AI ChatTest", "resetSaveSuccess 호출됨")
        _saveSuccess.value = null
    }

    // ─────────────────────────────────────────────
    //            Internal utilities
    // ─────────────────────────────────────────────

    /** http/https/gs:///상대경로(%2F 포함)를 모두 downloadUrl로 통일 */
    private suspend fun resolveImageUrl(raw: String): String {
        if (raw.startsWith("http", ignoreCase = true)) return raw
        val ref = if (raw.startsWith("gs://", ignoreCase = true)) {
            storage.getReferenceFromUrl(raw)
        } else {
            val path = raw.replace("%2F", "/").trimStart('/')
            storage.reference.child(path)
        }
        return ref.downloadUrl.await().toString()
    }

    /** WriteScreen이 만드는 포맷으로 통일: order/태그/메타 보정 */
    private fun normalizeRecipeForWriteScreen(
        original: RecipeItem,
        ownerUid: String,
        newDocId: String
    ): RecipeItem {

        fun canonicalizeOrder(raw: String): String {
            if (raw.isBlank()) return raw

            // 라인 분리(○, 개행, 불릿 등)
            val chunks = raw.split("○", "\n", "•", "-", "·")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            // 이미 "n." 또는 "n)"로 시작하면 접두만 보장하여 합치기
            val looksIndexed = chunks.all { it.matches(Regex("""^\d+[\.\)]\s*.*""")) }
            if (looksIndexed) {
                return chunks.mapIndexed { idx, s ->
                    "○${idx + 1}." + s.replaceFirst(Regex("""^\d+[\.\)]\s*"""), "")
                }.joinToString(" ")
            }

            // (제목) 설명 형태로 캐스팅 (방법/시간은 원문 끝의 "(방법,HH:MM:SS)"가 있을 때 자동 포함)
            val leadIndexRe = Regex("""^\s*(?:[○\-\•\·]?\s*)?\d+[\.\)]\s*""")
            val methodTimeRe = Regex("""\(([^()]+?),\s*([0-9]{2}:[0-9]{2}:[0-9]{2})\)\s*$""")

            return chunks.mapIndexed { idx, rawLine ->
                var line = rawLine.replaceFirst(leadIndexRe, "").trim()

                // 끝의 (방법,시간) 분리
                val mt = methodTimeRe.find(line)
                var method: String? = null
                var time: String? = null
                if (mt != null) {
                    method = mt.groupValues[1].trim()
                    time = mt.groupValues[2].trim()
                    line = line.removeRange(mt.range).trim()
                }

                // 제목/설명 분리(우선 ':' 시도 후 fallback)
                val sepIdx = line.indexOf(':').takeIf { it > 0 } ?: -1
                val title = if (sepIdx > 0) line.substring(0, sepIdx).trim()
                else line.take(18).replace("\n", " ").trim()
                val desc = if (sepIdx > 0) line.substring(sepIdx + 1).trim() else line

                buildString {
                    append("○${idx + 1}.(").append(title).append(") ").append(desc)
                    if (!method.isNullOrBlank() && !time.isNullOrBlank()) {
                        append(" (").append(method).append(",").append(time).append(")")
                    }
                }
            }.joinToString(" ")
        }

        fun normalizeTags(tags: List<String>): List<String> =
            tags.map { if (it.startsWith("#")) it else "#$it" }.distinct()

        val safeDuration = original.duration.coerceAtLeast(0)
        val safeCost = if (original.cost > 0) original.cost else original.priceInSalt

        return original.copy(
            id = newDocId, //  문서 ID ↔ recipe.id 일치
            order = canonicalizeOrder(original.order),
            tags = normalizeTags(original.tags),
            C_categories = if (original.C_categories.isEmpty()) listOf("AI") else original.C_categories,
            priceInSalt = original.priceInSalt.coerceAtLeast(0),
            cost = safeCost.coerceAtLeast(0),
            duration = safeDuration,
            authorId = if (original.authorId.isBlank()) ownerUid else original.authorId
            // authorName은 필요 시 호출부에서 주입
        )
    }
}
