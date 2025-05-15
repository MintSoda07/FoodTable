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
    private var isEmailVerified = false
    private var isNicknameValid = false
    private val temporaryPassword = "TemporaryPass123!"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val email = remember { mutableStateOf("") }
            val nickname = remember { mutableStateOf("") }
            val password = remember { mutableStateOf("") }
            val confirmPassword = remember { mutableStateOf("") }
            val warningText = remember { mutableStateOf("") }

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
                emailVerified = isEmailVerified,
                nicknameValid = isNicknameValid,
                onEmailVerifyClick = {
                    if (email.value.isNotEmpty()) {
                        sendEmailVerification(email.value) { warningText.value = it }
                    } else {
                        warningText.value = "이메일을 입력하세요."
                    }
                },
                onNicknameCheckClick = {
                    if (nickname.value.isNotEmpty()) {
                        checkNicknameAvailability(nickname.value) { warningText.value = it }
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
                        !isNicknameValid -> warningText.value = "닉네임 중복 확인을 완료하세요."
                        !isEmailVerified -> warningText.value = "이메일 인증을 먼저 완료하세요."
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

    private fun sendEmailVerification(email: String, onError: (String) -> Unit) {
        auth.fetchSignInMethodsForEmail(email).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val signInMethods = task.result?.signInMethods ?: emptyList()
                if (signInMethods.isNotEmpty()) {
                    onError("이미 사용 중인 이메일입니다.")
                    return@addOnCompleteListener
                }
                auth.createUserWithEmailAndPassword(email, temporaryPassword).addOnCompleteListener { task2 ->
                    if (task2.isSuccessful) {
                        val user = auth.currentUser
                        user?.sendEmailVerification()?.addOnCompleteListener { verificationTask ->
                            if (verificationTask.isSuccessful) {
                                Toast.makeText(this, "이메일 인증 링크가 전송되었습니다.", Toast.LENGTH_LONG).show()
                                isEmailVerified = true
                            } else {
                                onError("이메일 인증 링크 전송 실패")
                            }
                        }
                    } else {
                        onError("이메일 계정 생성 실패. 이미 등록된 이메일일 수 있습니다.")
                    }
                }
            } else {
                onError("이메일 확인 중 오류 발생")
            }
        }
    }

    private fun checkNicknameAvailability(nickname: String, onError: (String) -> Unit) {
        firestore.collection("user")
            .whereEqualTo("name", nickname)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "사용 가능한 닉네임입니다.", Toast.LENGTH_SHORT).show()
                    isNicknameValid = true
                } else {
                    onError("이미 사용 중인 닉네임입니다.")
                    isNicknameValid = false
                }
            }
            .addOnFailureListener {
                onError("닉네임 확인 중 오류 발생")
                isNicknameValid = false
            }
    }

    private fun updateUserPasswordAndRegister(
        email: String,
        newPassword: String,
        nickname: String,
        onError: (String) -> Unit
    ) {
        val user = auth.currentUser ?: return onError("이메일 인증 후 다시 시도하세요.")

        val credential = EmailAuthProvider.getCredential(email, temporaryPassword)
        user.reauthenticate(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                user.updatePassword(newPassword).addOnCompleteListener { passwordTask ->
                    if (passwordTask.isSuccessful) {
                        createUser(email, newPassword, nickname, onError)
                    } else {
                        onError("비밀번호 변경 실패")
                    }
                }
            } else {
                onError("재인증 실패: ${task.exception?.message}")
            }
        }
    }

    private fun createUser(
        email: String,
        password: String,
        nickname: String,
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
            .addOnFailureListener {
                onError("회원가입 실패")
            }
    }
}
