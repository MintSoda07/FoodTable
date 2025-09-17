package com.bcu.foodtable

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.*
import com.bcu.foodtable.ui.home.FoodTableTheme
import com.bcu.foodtable.useful.ActivityTransition
import com.bcu.foodtable.useful.User
import com.bcu.foodtable.useful.UserManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.messaging.FirebaseMessaging
import com.bcu.foodtable.JetpackCompose.LoginViewModel

// 실제 경로로 교체
import com.bcu.foodtable.ui.merchant.MerchantPage
import com.bcu.foodtable.ui.merchant.MerchantSetupActivity
import com.bcu.foodtable.JetpackCompose.biometric.debugDumpBio
import com.bcu.foodtable.ui.LoginScreenImproved

class LoginActivity : AppCompatActivity() {

    private val bioVm by viewModels<LoginViewModel>()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val isDebugging = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 앱 진입 시 상태 확인(선택)
        debugDumpBio(applicationContext)

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
                        handleLogin(
                            email = email,
                            password = password,
                            isAutoLogin = isAutoLogin,
                            onResult = { result ->
                                warning = result
                                isLoading = false
                            },
                            registerBiometric = false // 🔴 일반 로그인에서는 생체 등록 수행하지 않음(프롬프트 X)
                        )
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
                    onGoogleLoginClick = { warning = "구글 로그인은 현재 지원되지 않습니다." },
                    onKakaoLoginClick  = { warning = "카카오 로그인은 현재 지원되지 않습니다." },
                    onBiometricLoginWithToken = { token -> signInWithBiometricToken(token) },
                    onDebugBio = { ctx -> debugDumpBio(ctx) },
                    vm = bioVm
                )
            }
        }
    }

    private fun handleLogin(
        email: String,
        password: String,
        isAutoLogin: Boolean,
        onResult: (String) -> Unit,
        registerBiometric: Boolean = false // 🔴 기본값도 false로: 자동 등록 막기
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
                        if (!task.isSuccessful) {
                            Log.e("Login", "[LOGIN] 실패: ${task.exception?.localizedMessage}", task.exception)
                            onResult(task.exception?.localizedMessage ?: getString(R.string.login_failure))
                            return@addOnCompleteListener
                        }

                        val user = auth.currentUser ?: run {
                            onResult(getString(R.string.login_failure))
                            return@addOnCompleteListener
                        }
                        Log.d("Login", "[LOGIN] 성공: uid=${user.uid}, emailVerified=${user.isEmailVerified}")

                        if (user.isEmailVerified == false && !isDebugging) {
                            Log.w("Login", "[LOGIN] 이메일 인증 미완료, 로그인 거부")
                            onResult(getString(R.string.email_not_verified_warning))
                            return@addOnCompleteListener
                        }

                        // ✅ UI 스피너 해제/경고 초기화 먼저
                        onResult("")

                        // 🔕 일반 로그인에서는 생체 등록을 자동 실행하지 않음
                        // if (registerBiometric) { ... }  // ← 필요 시 나중에 수동 등록 버튼에서 호출

                        proceedAfterLogin(user.uid, email, password, isAutoLogin)
                    }
            }
        }
    }

    /** (선택) 수동 등록용: 설정/버튼에서 명시적으로 호출하고 싶을 때 사용 */
    private fun requestBiometricEnroll(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(this, "이메일/비밀번호를 먼저 입력해 주세요.", Toast.LENGTH_SHORT).show()
            return
        }
        val payload = "$email::$password".toByteArray(Charsets.UTF_8)
        bioVm.registerBiometric(this, payload) { _ ->
            debugDumpBio(applicationContext)
            bioVm.refreshBiometricState(this)
            Toast.makeText(this, "생체 로그인 등록이 완료되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    /** 이메일/비번/바이오 공통 후속 처리 */
    private fun proceedAfterLogin(uid: String, email: String, password: String, isAutoLogin: Boolean) {
        // ✅ 자동 로그인 상태 저장
        val prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
        if (isAutoLogin) {
            prefs.edit()
                .putBoolean("AUTO_LOGIN", true)
                .putString("EMAIL", email)
                .putString("PASSWORD", password)
                .apply()
        } else {
            prefs.edit().clear().apply()
        }

        // 유저 문서 보정 후 진행
        ensureUserDocPatched(
            uid = uid,
            onReady = {
                fetchUserData(
                    uid = uid,
                    onSuccess = { userData ->
                        UserManager.setUser(
                            userData.name, userData.email, userData.image,
                            userData.phoneNumber, userData.point,
                            uid, userData.rankPoint, userData.description,
                            userData.location, userData.manager, userData.fcmtoken
                        )

                        // ✅ FCM 토큰 갱신 후 역할 분기
                        FirebaseMessaging.getInstance().token
                            .addOnSuccessListener { token ->
                                FirebaseFirestore.getInstance()
                                    .collection("user")
                                    .document(uid)
                                    .update("fcmToken", token)
                                    .addOnFailureListener { e ->
                                        Log.e("Login", "[FCM] 저장 실패: ${e.message}", e)
                                    }
                                routeAfterLogin(uid)
                            }
                            .addOnFailureListener { e ->
                                Log.e("Login", "[FCM] 토큰 획득 실패: ${e.message}", e)
                                routeAfterLogin(uid)
                            }
                    },
                    onFailure = { exception ->
                        Log.e("Login", "[LOGIN] fetchUserData 실패: ${exception.localizedMessage}", exception)
                        Toast.makeText(
                            this,
                            getString(R.string.login_failure) + ": " + exception.localizedMessage,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            },
            onError = { e ->
                Log.e("Login", "[LOGIN] ensureUserDocPatched 실패: ${e.message}", e)
                routeAfterLogin(uid)
            }
        )
    }

    // 생체 로그인(복호화→자동 로그인)
    private fun signInWithBiometricToken(token: ByteArray) {
        try {
            val raw = token.toString(Charsets.UTF_8)
            Log.d("BIO", "biometric payload len=${raw.length}")

            val sep = raw.indexOf("::")
            if (sep <= 0) {
                Log.e("BIO", "invalid payload (no ::)")
                Toast.makeText(
                    this,
                    "생체 로그인 데이터가 올바르지 않습니다. 이메일/비밀번호로 한 번 로그인해 주세요.",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            val email = raw.substring(0, sep).trim()
            val pass  = raw.substring(sep + 2) // 비밀번호는 공백 보존

            if (email.isEmpty() || pass.isEmpty()) {
                Log.e("BIO", "parsed empty field (emailEmpty=${email.isEmpty()}, passEmpty=${pass.isEmpty()})")
                Toast.makeText(this, "저장된 계정 정보가 비어 있습니다. 다시 로그인해 주세요.", Toast.LENGTH_SHORT).show()
                return
            }

            Log.d("BIO", "parsed email=$email, passLen=${pass.length}; autoLogin=false")
            Toast.makeText(this, "생체 인증 성공: 자동 로그인 중…", Toast.LENGTH_SHORT).show()

            // 🔒 바이오 경로는 항상 재등록 없이 바로 로그인만
            handleLogin(
                email = email,
                password = pass,
                isAutoLogin = false,
                onResult = { result ->
                    if (result.isNotEmpty()) {
                        Log.e("BIO", "auto login failed: $result")
                        Toast.makeText(this, result, Toast.LENGTH_LONG).show()
                    } else {
                        Log.d("BIO", "auto login success")
                    }
                },
                registerBiometric = false
            )
        } catch (t: Throwable) {
            Log.e("BIO", "signInWithBiometricToken() crashed", t)
            Toast.makeText(this, "생체 로그인 처리 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show()
        }
    }

    // 로그인 직후 user/{uid} 문서 보정 (uid 세팅 + roles 기본값)
    private fun ensureUserDocPatched(
        uid: String,
        onReady: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        val ref = db.collection("user").document(uid)

        ref.get()
            .addOnSuccessListener { snap ->
                val base = hashMapOf(
                    "uid" to uid,
                    "lastLoginAt" to FieldValue.serverTimestamp()
                )
                if (snap.exists()) {
                    val hasRoles = (snap.get("roles") as? List<*>)?.isNotEmpty() == true
                    val patch = if (hasRoles) base else base + ("roles" to listOf("user"))
                    ref.set(patch, SetOptions.merge())
                        .addOnSuccessListener { onReady() }
                        .addOnFailureListener(onError)
                } else {
                    val create = base + mapOf(
                        "email" to (auth.currentUser?.email ?: ""),
                        "name" to (auth.currentUser?.displayName ?: ""),
                        "image" to (auth.currentUser?.photoUrl?.toString() ?: ""),
                        "phoneNumber" to (auth.currentUser?.phoneNumber ?: ""),
                        "point" to 0,
                        "rankPoint" to 0,
                        "roles" to listOf("user")
                    )
                    ref.set(create)
                        .addOnSuccessListener { onReady() }
                        .addOnFailureListener(onError)
                }
            }
            .addOnFailureListener(onError)
    }

    // ✅ roles / storeId / storeName 기준으로 분기
    private fun routeAfterLogin(uid: String) {
        FirebaseFirestore.getInstance()
            .collection("user")
            .document(uid)
            .get()
            .addOnSuccessListener { snap ->
                val roles = (snap.get("roles") as? List<*>)?.map { it.toString() } ?: emptyList()
                val storeId = snap.getString("storeId")
                val storeName = snap.getString("storeName").orEmpty()

                when {
                    "merchant" in roles && (storeId.isNullOrBlank() || storeName.isBlank()) -> {
                        ActivityTransition.startStatic(this@LoginActivity, MerchantSetupActivity::class.java)
                    }
                    "merchant" in roles -> {
                        ActivityTransition.startStatic(this@LoginActivity, MerchantPage::class.java)
                    }
                    else -> {
                        ActivityTransition.startStatic(this@LoginActivity, HomeActivity::class.java)
                    }
                }
                finish()
            }
            .addOnFailureListener { e ->
                Log.e("Login", "[LOGIN] roles 조회 실패: ${e.message}", e)
                Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT).show()
                ActivityTransition.startStatic(this@LoginActivity, HomeActivity::class.java)
                finish()
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
            .addOnFailureListener { exception -> onFailure(exception) }
    }
}
