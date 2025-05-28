package com.bcu.foodtable

import LoginScreenImproved
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.bcu.foodtable.ui.home.FoodTableTheme
import com.bcu.foodtable.useful.ActivityTransition
import com.bcu.foodtable.useful.User
import com.bcu.foodtable.useful.UserManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : ComponentActivity() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val isDebugging = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FoodTableTheme {
                var email by remember { mutableStateOf("") }
                var password by remember { mutableStateOf("") }
                var warning by remember { mutableStateOf("") }
                var isLoading by remember { mutableStateOf(false) }
                var isAutoLogin by remember { mutableStateOf(false) }

                // ✅ 자동 로그인 상태 불러오기
                LaunchedEffect(Unit) {
                    val prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
                    isAutoLogin = prefs.getBoolean("AUTO_LOGIN", false)
                    if (isAutoLogin) {
                        email = prefs.getString("EMAIL", "") ?: ""
                        password = prefs.getString("PASSWORD", "") ?: ""
                    }
                }

                LoginScreenImproved(
                    email = email,
                    password = password,
                    isAutoLogin = isAutoLogin,
                    onEmailChange = { email = it },
                    onPasswordChange = { password = it },
                    onAutoLoginChange = { isAutoLogin = it },
                    warningText = warning,
                    isLoggingIn = isLoading,
                    onLoginClick = {
                        isLoading = true
                        handleLogin(email, password, isAutoLogin) { result ->
                            warning = result
                            isLoading = false
                        }
                    },
                    onSignUpClick = {
                        ActivityTransition.startStatic(this@LoginActivity, SignUpActivity::class.java)
                    },
                    onForgotPasswordClick = {
                        if (email.isBlank()) {
                            warning = "비밀번호를 재설정할 이메일 주소를 입력해주세요."
                            return@LoginScreenImproved
                        }
                        isLoading = true
                        auth.sendPasswordResetEmail(email)
                            .addOnCompleteListener { task ->
                                isLoading = false
                                warning = if (task.isSuccessful) {
                                    "비밀번호 재설정 이메일을 보냈습니다. 이메일을 확인해주세요."
                                } else {
                                    task.exception?.localizedMessage ?: "비밀번호 재설정 이메일 전송에 실패했습니다."
                                }
                            }
                    },
                    onGoogleLoginClick = {
                        warning = "구글 로그인은 현재 지원되지 않습니다."
                    },
                    onKakaoLoginClick = {
                        warning = "카카오 로그인은 현재 지원되지 않습니다."
                    }
                )
            }
        }
    }

    private fun handleLogin(email: String, password: String, isAutoLogin: Boolean, onResult: (String) -> Unit) {
        when {
            email.isBlank() -> onResult(getString(R.string.id_empty_warning))
            password.isBlank() -> onResult(getString(R.string.pwd_empty_warning))
            else -> {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            if (user?.isEmailVerified == false && !isDebugging) {
                                onResult(getString(R.string.email_not_verified_warning))
                                return@addOnCompleteListener
                            }

                            // ✅ 자동 로그인 상태 저장
                            if (isAutoLogin) {
                                val prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
                                prefs.edit()
                                    .putBoolean("AUTO_LOGIN", true)
                                    .putString("EMAIL", email)
                                    .putString("PASSWORD", password)
                                    .apply()
                            } else {
                                val prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
                                prefs.edit().clear().apply()
                            }

                            fetchUserData(
                                uid = user!!.uid,
                                onSuccess = { userData ->
                                    UserManager.setUser(
                                        userData.name, userData.email, userData.image,
                                        userData.phoneNumber, userData.point,
                                        userData.uid, userData.rankPoint, userData.description, userData.location
                                    )
                                    Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT).show()
                                    ActivityTransition.startStatic(this@LoginActivity, HomeActivity::class.java)
                                    finish()
                                },
                                onFailure = { exception ->
                                    onResult(getString(R.string.login_failure) + ": " + exception.localizedMessage)
                                }
                            )
                        } else {
                            onResult(task.exception?.localizedMessage ?: getString(R.string.login_failure))
                        }
                    }
            }
        }
    }

    private fun fetchUserData(
        uid: String,
        onSuccess: (User) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        FirebaseFirestore.getInstance()
            .collection("user")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val user = document.toObject(User::class.java)
                    if (user != null) {
                        user.uid = uid
                        onSuccess(user)
                    } else {
                        onFailure(Exception("사용자 데이터 변환 실패 (null 반환)"))
                    }
                } else {
                    onFailure(Exception("사용자 문서를 찾을 수 없습니다."))
                }
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }
}
