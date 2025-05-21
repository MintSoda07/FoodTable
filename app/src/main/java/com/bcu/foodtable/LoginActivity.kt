package com.bcu.foodtable

import LoginScreen
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
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
            var email by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }
            var warning by remember { mutableStateOf("") }

            LoginScreen(
                email = email,
                password = password,
                onEmailChange = { email = it },
                onPasswordChange = { password = it },
                warningText = warning,
                onLoginClick = {
                    handleLogin(email, password) { result ->
                        warning = result
                    }
                },
                onSignUpClick = {
                    ActivityTransition.startStatic(this, SignUpActivity::class.java)
                }
            )
//                warningText = warning
//            )
        }
    }

    private fun handleLogin(email: String, password: String, onResult: (String) -> Unit) {
        val pattern = Regex("^(?=.*[A-Z])(?=.*[!@#\$%^&*()\\-+=]).{6,48}$")

        when {
            email.isBlank() -> onResult(getString(R.string.id_empty_warning))
            password.isBlank() -> onResult(getString(R.string.pwd_empty_warning))
            !password.matches(pattern) -> onResult(getString(R.string.pwd_validate_warning))
            else -> {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            if (user?.isEmailVerified == false && !isDebugging) {
                                onResult(getString(R.string.email_not_verified_warning))
                                return@addOnCompleteListener
                            }
                            fetchUserData(
                                uid = user!!.uid,
                                onSuccess = { userData ->
                                    UserManager.setUser(
                                        userData.name, userData.email, userData.image,
                                        userData.phoneNumber, userData.point,
                                        userData.uid, userData.rankPoint, userData.description
                                    )
                                    Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT).show()
                                    ActivityTransition.startStatic(this, HomeActivity::class.java)
                                },
                                onFailure = {
                                    onResult(getString(R.string.login_failure))
                                }
                            )
                        } else {
                            onResult(getString(R.string.login_failure))
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
                val user = document.toObject(User::class.java)
                if (user != null) {
                    user.uid = uid
                    onSuccess(user)
                } else {
                    onFailure(Exception("사용자 데이터 변환 실패"))
                }
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }
}