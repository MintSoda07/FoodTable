package com.bcu.foodtable.ui.merchant

import androidx.compose.runtime.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

enum class StoreRole { OWNER, MANAGER, STAFF, VIEWER }

data class StaffProfile(
    val uid: String = "",
    val name: String = "",
    val role: StoreRole = StoreRole.STAFF,
    val phone: String = "",
    val email: String = "",
    val active: Boolean = true
)

/** 현재 로그인 사용자의 상점 내 역할을 로드 */
@Composable
fun rememberMyStoreRole(storeId: String, ownerUidFromStore: String? = null): State<StoreRole> {
    val db = Firebase.firestore
    val myUid = Firebase.auth.currentUser?.uid.orEmpty()
    val roleState = remember { mutableStateOf(StoreRole.VIEWER) }

    LaunchedEffect(storeId, myUid) {
        if (storeId.isBlank() || myUid.isBlank()) {
            roleState.value = StoreRole.VIEWER
            return@LaunchedEffect
        }
        // 1) 상점 문서의 ownerUid면 OWNER
        if (!ownerUidFromStore.isNullOrBlank() && ownerUidFromStore == myUid) {
            roleState.value = StoreRole.OWNER
            return@LaunchedEffect
        } else {
            // 없으면 상점 문서에서 가져오기
            db.collection("merchants").document(storeId).get().addOnSuccessListener { snap ->
                val owner = snap.getString("ownerUid")
                if (owner == myUid) {
                    roleState.value = StoreRole.OWNER
                } else {
                    // 2) staff 서브컬렉션에서 내 문서를 찾아 역할 확인
                    db.collection("merchants").document(storeId)
                        .collection("staff").document(myUid)
                        .get().addOnSuccessListener { st ->
                            val r = when (st.getString("role")) {
                                "OWNER" -> StoreRole.OWNER
                                "MANAGER" -> StoreRole.MANAGER
                                "STAFF" -> StoreRole.STAFF
                                else -> StoreRole.VIEWER
                            }
                            val active = st.getBoolean("active") ?: true
                            roleState.value = if (active) r else StoreRole.VIEWER
                        }.addOnFailureListener { roleState.value = StoreRole.VIEWER }
                }
            }.addOnFailureListener { roleState.value = StoreRole.VIEWER }
        }
    }
    return roleState
}

fun canEditCoupons(role: StoreRole) = role == StoreRole.OWNER || role == StoreRole.MANAGER
fun canEditStaff(role: StoreRole) = role == StoreRole.OWNER || role == StoreRole.MANAGER
fun canChangeRole(actor: StoreRole, targetOld: StoreRole, targetNew: StoreRole): Boolean {
    // OWNER만 다른 사람을 OWNER로 승격/강등 가능. MANAGER는 STAFF <-> MANAGER 변경까지.
    return when (actor) {
        StoreRole.OWNER -> true
        StoreRole.MANAGER -> (targetNew != StoreRole.OWNER && targetOld != StoreRole.OWNER)
        else -> false
    }
}
