package com.bcu.foodtable

import SigupScreen
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.bcu.foodtable.useful.User as UserData
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignUpActivity : ComponentActivity() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val temporaryPassword = "TemporaryPass123!"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val email = remember { mutableStateOf("") }
            val nickname = remember { mutableStateOf("") }
            val password = remember { mutableStateOf("") }
            val confirmPassword = remember { mutableStateOf("") }
            val warningText = remember { mutableStateOf("") }

            // ✅ Compose 상태로 관리 (UI가 즉시 반영)
            val emailVerified = remember { mutableStateOf(false) }
            val emailVerificationSent = remember { mutableStateOf(false) }
            val nicknameValid = remember { mutableStateOf(false) }

            SigupScreen(
                email = email.value,
                onEmailChange = { email.value = it },
                nickname = nickname.value,
                onNicknameChange = { nickname.value = it },
                password = password.value,
                onPasswordChange = { password.value = it },
                confirmPassword = confirmPassword.value,
                onConfirmPasswordChange = { confirmPassword.value = it },
                warningText = warningText.value,
                emailVerified = emailVerified.value,
                emailVerificationSent = emailVerificationSent.value,   // ✅ 추가 prop
                nicknameValid = nicknameValid.value,
                onEmailVerifyClick = {
                    if (email.value.isNotEmpty()) {
                        sendEmailVerification(
                            email = email.value,
                            onSent = {
                                Toast.makeText(this, "인증 메일을 보냈습니다.", Toast.LENGTH_LONG).show()
                                emailVerificationSent.value = true
                            },
                            onError = { warningText.value = it }
                        )
                    } else {
                        warningText.value = "이메일을 입력하세요."
                    }
                },
                onCheckVerificationClick = {                          // ✅ 추가 prop
                    if (email.value.isNotEmpty()) {
                        checkEmailVerified(
                            email = email.value,
                            onResult = { ok ->
                                emailVerified.value = ok
                                if (ok) {
                                    Toast.makeText(this, "이메일 인증 완료!", Toast.LENGTH_SHORT).show()
                                } else {
                                    warningText.value = "아직 인증이 완료되지 않았습니다. 메일의 링크를 눌러주세요."
                                }
                            },
                            onError = { warningText.value = it }
                        )
                    } else {
                        warningText.value = "이메일을 입력하세요."
                    }
                },
                onNicknameCheckClick = {
                    if (nickname.value.isNotEmpty()) {
                        checkNicknameAvailability(nickname.value) { ok, msg ->
                            nicknameValid.value = ok
                            if (!ok) warningText.value = msg
                            else Toast.makeText(this, "사용 가능한 닉네임입니다.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        warningText.value = "닉네임을 입력하세요."
                    }
                },
                onSignUpClick = {
                    when {
                        email.value.isEmpty() -> warningText.value = "이메일을 입력하세요."
                        nickname.value.isEmpty() -> warningText.value = "닉네임을 입력하세요."
                        password.value.isEmpty() -> warningText.value = "비밀번호를 입력하세요."
                        confirmPassword.value.isEmpty() -> warningText.value = "비밀번호 확인을 입력하세요."
                        password.value != confirmPassword.value -> warningText.value = "비밀번호가 일치하지 않습니다."
                        !nicknameValid.value -> warningText.value = "닉네임 중복 확인을 완료하세요."
                        !emailVerified.value -> warningText.value = "이메일 인증을 먼저 완료하세요."
                        else -> updateUserPasswordAndRegister(
                            email = email.value,
                            newPassword = password.value,
                            nickname = nickname.value,
                            onError = { warningText.value = it }
                        )
                    }
                }
            )
        }
    }

    /** 1) 인증 메일 보내기: 임시 계정 생성 후 sendEmailVerification */
    private fun sendEmailVerification(
        email: String,
        onSent: () -> Unit,
        onError: (String) -> Unit
    ) {
        // 이미 존재하는 이메일인지 확인
        auth.fetchSignInMethodsForEmail(email).addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                onError("이메일 확인 중 오류 발생")
                return@addOnCompleteListener
            }
            val signInMethods = task.result?.signInMethods ?: emptyList()
            if (signInMethods.isNotEmpty()) {
                onError("이미 사용 중인 이메일입니다.")
                return@addOnCompleteListener
            }

            // 임시 계정 생성 → 인증 메일 발송
            auth.createUserWithEmailAndPassword(email, temporaryPassword)
                .addOnCompleteListener { createTask ->
                    if (!createTask.isSuccessful) {
                        onError("이메일 계정 생성 실패. 이미 등록된 이메일일 수 있습니다.")
                        return@addOnCompleteListener
                    }
                    val user = auth.currentUser
                    user?.sendEmailVerification()?.addOnCompleteListener { verificationTask ->
                        if (verificationTask.isSuccessful) {
                            onSent()
                        } else {
                            onError("이메일 인증 링크 전송 실패")
                        }
                    } ?: onError("사용자 정보가 없습니다.")
                }
        }
    }

    /** 2) 인증 완료 확인: reload() 후 emailVerified 확인 */
    private fun checkEmailVerified(
        email: String,
        onResult: (Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        val current = auth.currentUser
        // 현재 로그인 유저가 없거나 다른 이메일이면 임시 비번으로 로그인해서 확인
        if (current == null || current.email != email) {
            auth.signInWithEmailAndPassword(email, temporaryPassword)
                .addOnCompleteListener { signInTask ->
                    if (!signInTask.isSuccessful) {
                        onError("임시 계정 로그인 실패. 인증 메일을 다시 보내세요.")
                        return@addOnCompleteListener
                    }
                    auth.currentUser?.reload()?.addOnCompleteListener { reloadTask ->
                        if (!reloadTask.isSuccessful) {
                            onError("인증 상태 갱신 실패")
                        } else {
                            onResult(auth.currentUser?.isEmailVerified == true)
                        }
                    }
                }
            return
        }

        // 동일 사용자면 바로 reload
        current.reload().addOnCompleteListener { reloadTask ->
            if (!reloadTask.isSuccessful) {
                onError("인증 상태 갱신 실패")
            } else {
                onResult(auth.currentUser?.isEmailVerified == true)
            }
        }
    }

    private fun checkNicknameAvailability(
        nickname: String,
        callback: (ok: Boolean, msg: String) -> Unit
    ) {
        firestore.collection("user")
            .whereEqualTo("name", nickname)
            .get()
            .addOnSuccessListener { docs ->
                if (docs.isEmpty) callback(true, "")
                else callback(false, "이미 사용 중인 닉네임입니다.")
            }
            .addOnFailureListener {
                callback(false, "닉네임 확인 중 오류 발생")
            }
    }

    /** 3) 최종 가입: 임시 비번 → 사용자 비번으로 변경 후 user 문서 생성 */
    private fun updateUserPasswordAndRegister(
        email: String,
        newPassword: String,
        nickname: String,
        onError: (String) -> Unit
    ) {
        val user = auth.currentUser ?: return onError("이메일 인증 후 다시 시도하세요.")
        val credential = EmailAuthProvider.getCredential(email, temporaryPassword)

        user.reauthenticate(credential).addOnCompleteListener { reTask ->
            if (!reTask.isSuccessful) {
                onError("재인증 실패: ${reTask.exception?.message}")
                return@addOnCompleteListener
            }
            user.updatePassword(newPassword).addOnCompleteListener { pwTask ->
                if (!pwTask.isSuccessful) {
                    onError("비밀번호 변경 실패")
                    return@addOnCompleteListener
                }
                createUserDoc(nickname, email, onError)
            }
        }
    }

    private fun createUserDoc(
        nickname: String,
        email: String,
        onError: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: return onError("사용자 정보 오류")
        val user = UserData(
            name = nickname,
            email = email,
            image = "",
            phoneNumber = "",
            point = 0,
            rankPoint = 0,
            description = ""
        )
        firestore.collection("user").document(uid).set(user)
            .addOnSuccessListener {
                Toast.makeText(this, "회원가입 성공!", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener { onError("회원가입 실패") }
    }
}
