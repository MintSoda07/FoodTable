package com.bcu.foodtable

import LoginScreenImproved
import android.os.Bundle
import android.util.Log
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
import com.google.firebase.messaging.FirebaseMessaging

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

    private fun handleLogin(
        email: String,
        password: String,
        isAutoLogin: Boolean,
        onResult: (String) -> Unit
    ) {
        when {
            email.isBlank() -> {
                Log.w("Login", "[LOGIN] 이메일 입력 안 됨")
                onResult(getString(R.string.id_empty_warning))
            }
            password.isBlank() -> {
                Log.w("Login", "[LOGIN] 비밀번호 입력 안 됨")
                onResult(getString(R.string.pwd_empty_warning))
            }
            else -> {
                Log.d("Login", "[LOGIN] 이메일/비번 로그인 시도: $email")
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            Log.d("Login", "[LOGIN] 성공: uid=${user?.uid}, emailVerified=${user?.isEmailVerified}")
                            if (user?.isEmailVerified == false && !isDebugging) {
                                Log.w("Login", "[LOGIN] 이메일 인증 미완료, 로그인 거부")
                                onResult(getString(R.string.email_not_verified_warning))
                                return@addOnCompleteListener
                            }

                            // ✅ 자동 로그인 상태 저장
                            val prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
                            if (isAutoLogin) {
                                prefs.edit()
                                    .putBoolean("AUTO_LOGIN", true)
                                    .putString("EMAIL", email)
                                    .putString("PASSWORD", password)
                                    .apply()
                                Log.d("Login", "[LOGIN] 자동로그인 설정 저장됨")
                            } else {
                                prefs.edit().clear().apply()
                                Log.d("Login", "[LOGIN] 자동로그인 정보 삭제됨")
                            }

                            // ✅ User 정보 fetch → FCM 토큰 없는 경우에만 등록
                            fetchUserData(
                                uid = user!!.uid,
                                onSuccess = { userData ->
                                    Log.d("Login", "[LOGIN] fetchUserData 성공: $userData")
                                    UserManager.setUser(
                                        userData.name, userData.email, userData.image,
                                        userData.phoneNumber, userData.point,
                                        userData.uid, userData.rankPoint, userData.description, userData.location, userData.manager,userData.fcmtoken
                                    )

                                    // (1) FCM 토큰 이미 저장되어 있나?
                                    val existingFcmToken = userData.fcmtoken
                                    if (existingFcmToken.isNullOrBlank()) {
                                        Log.d("Login", "[FCM] FCM 토큰 없음 → 새로 요청 및 Firestore 저장")
                                        FirebaseMessaging.getInstance().token
                                            .addOnSuccessListener { token ->
                                                Log.d("Login", "[FCM] 토큰 획득: $token")
                                                FirebaseFirestore.getInstance().collection("user")
                                                    .document(userData.uid)
                                                    .update("fcmToken", token)
                                                    .addOnSuccessListener {
                                                        Log.d("Login", "[FCM] Firestore 토큰 저장 성공")
                                                    }
                                                    .addOnFailureListener { e ->
                                                        Log.e("Login", "[FCM] Firestore 토큰 저장 실패: ${e.message}", e)
                                                    }
                                            }
                                            .addOnFailureListener { e ->
                                                Log.e("Login", "[FCM] 토큰 획득 실패: ${e.message}", e)
                                            }
                                    } else {
                                        Log.d("Login", "[FCM] 이미 FCM 토큰 있음, Firestore 갱신 생략")
                                    }

                                    Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT).show()
                                    Log.d("Login", "[LOGIN] HomeActivity 이동")
                                    ActivityTransition.startStatic(this@LoginActivity, HomeActivity::class.java)
                                    finish()
                                },
                                onFailure = { exception ->
                                    Log.e("Login", "[LOGIN] fetchUserData 실패: ${exception.localizedMessage}", exception)
                                    onResult(getString(R.string.login_failure) + ": " + exception.localizedMessage)
                                }
                            )
                        } else {
                            Log.e("Login", "[LOGIN] 실패: ${task.exception?.localizedMessage}", task.exception)
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
