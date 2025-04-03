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
            var id by remember { mutableStateOf("") }
            var pwd by remember { mutableStateOf("") }
            var warning by remember { mutableStateOf("") }

            LoginScreen(
                idInput = id,
                onIdChange = { id = it },
                pwdInput = pwd,
                onPwdChange = { pwd = it },
                warningText = warning,
                onLoginClick = {
                    val passwordPattern = Regex("^(?=.*[A-Z])(?=.*[!@#\$%^&*()\\-+=]).{6,48}$")
                    when {
                        id.isEmpty() -> warning = getString(R.string.id_empty_warning)
                        pwd.isEmpty() -> warning = getString(R.string.pwd_empty_warning)
                        !pwd.matches(passwordPattern) -> warning = getString(R.string.pwd_validate_warning)
                        else -> {
                            auth.signInWithEmailAndPassword(id, pwd)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val user = auth.currentUser
                                        if (user?.isEmailVerified == false && !isDebugging) {
                                            warning = getString(R.string.email_not_verified_warning)
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
                                                Toast.makeText(
                                                    this@LoginActivity,
                                                    R.string.login_success,
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                ActivityTransition.startStatic(this@LoginActivity, HomeAcitivity::class.java)
                                            },
                                            onFailure = {
                                                warning = getString(R.string.login_failure)
                                            }
                                        )
                                    } else {
                                        warning = getString(R.string.login_failure)
                                    }
                                }
                        }
                    }
                },
                onSignUpClick = {
                    ActivityTransition.startStatic(this, SignUpActivity::class.java)
                }
            )
        }
    }

    private fun fetchUserData(
        uid: String,
        onSuccess: (User) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        FirebaseFirestore.getInstance().collection("user").document(uid)
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
            .addOnFailureListener { onFailure(it) }
    }
}
