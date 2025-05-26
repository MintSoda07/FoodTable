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
    private val isDebugging = true // This flag is used in your handleLogin

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FoodTableTheme { // Wrap with your app's theme
                var email by remember { mutableStateOf("") }
                var password by remember { mutableStateOf("") }
                var warning by remember { mutableStateOf("") }
                var isLoading by remember { mutableStateOf(false) } // For loading indicators

                LoginScreenImproved(
                    email = email,
                    password = password,
                    onEmailChange = { email = it },
                    onPasswordChange = { password = it },
                    warningText = warning,
                    //isLoading = isLoading, // Pass isLoading state
                    onLoginClick = {
                        isLoading = true // Start loading
                        handleLogin(email, password) { result ->
                            warning = result
                            isLoading = false // Stop loading after result
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
                                if (task.isSuccessful) {
                                    warning = "비밀번호 재설정 이메일을 보냈습니다. 이메일을 확인해주세요."
                                } else {
                                    warning = task.exception?.localizedMessage ?: "비밀번호 재설정 이메일 전송에 실패했습니다."
                                }
                            }
                    },
                    onGoogleLoginClick = {
                        warning = "구글 로그인은 현재 지원되지 않습니다."
                        // TODO: Implement Google Sign-In
                    },
                    onKakaoLoginClick = {
                        warning = "카카오 로그인은 현재 지원되지 않습니다."
                        // TODO: Implement Kakao Sign-In
                    }
                )
            }
        }
    }

    private fun handleLogin(email: String, password: String, onResult: (String) -> Unit) {
        // Password pattern from your original code. Consider if it's still needed
        // if Firebase handles password policies, or if this is for client-side pre-validation.
        // val pattern = Regex("^(?=.*[A-Z])(?=.*[!@#\$%^&*()\\-+=]).{6,48}$")

        when {
            email.isBlank() -> {
                onResult(getString(R.string.id_empty_warning))
                // isLoading should be set to false here if it was true before calling handleLogin
            }
            password.isBlank() -> {
                onResult(getString(R.string.pwd_empty_warning))
                // isLoading should be set to false here
            }
            // !password.matches(pattern) -> {  // Client-side validation can be useful
            //     onResult(getString(R.string.pwd_validate_warning))
            //     // isLoading should be set to false here
            // }
            else -> {
                // isLoading is true at this point (set in onLoginClick)
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        // isLoading will be set to false by the calling lambda in onLoginClick
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            // Email verification check
                            if (user?.isEmailVerified == false && !isDebugging) {
                                onResult(getString(R.string.email_not_verified_warning))
                                return@addOnCompleteListener
                            }
                            // Fetch user data from Firestore
                            fetchUserData(
                                uid = user!!.uid,
                                onSuccess = { userData ->
                                    UserManager.setUser(
                                        userData.name, userData.email, userData.image,
                                        userData.phoneNumber, userData.point,
                                        userData.uid, userData.rankPoint, userData.description
                                    )
                                    Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT).show()
                                    ActivityTransition.startStatic(this@LoginActivity, HomeActivity::class.java)
                                    finish() // Finish LoginActivity after successful login and transition
                                    // onResult("") // Clear warning on success or specific success message
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
            .collection("user") // Ensure this collection name is correct
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val user = document.toObject(User::class.java)
                    if (user != null) {
                        user.uid = uid // Ensure UID is set if not mapped by toObject
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