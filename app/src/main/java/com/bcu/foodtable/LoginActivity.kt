package com.bcu.foodtable

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bcu.foodtable.useful.ActivityTransition
import com.bcu.foodtable.useful.User
import com.bcu.foodtable.useful.UserManager
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    lateinit var loginIdInputLayout: TextInputEditText
    lateinit var loginPwdInputLayout: TextInputEditText
    lateinit var loginWarningTextBox: TextView
    val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val submitBtn = findViewById<Button>(R.id.loginSubmitBtn)
        val signUpBtn = findViewById<Button>(R.id.loginSignUpBtn)
        val passwordPattern = "^(?=.*[A-Z])(?=.*[!@#\$%^&*()-+=]).{6,48}$" // 비밀번호 정규식

        loginIdInputLayout = findViewById(R.id.idInputBox)
        loginPwdInputLayout = findViewById(R.id.pwdInputBox)
        loginWarningTextBox = findViewById(R.id.warningText)

        submitBtn.setOnClickListener {
            when {
                loginIdInputLayout.text!!.isEmpty() -> {
                    loginIdInputLayout.requestFocus()
                    warning_about(R.string.id_empty_warning)
                }
                loginPwdInputLayout.text!!.isEmpty() -> {
                    loginPwdInputLayout.requestFocus()
                    warning_about(R.string.pwd_empty_warning)
                }
                !loginPwdInputLayout.text!!.matches(Regex(passwordPattern)) -> {
                    loginPwdInputLayout.requestFocus()
                    warning_about(R.string.pwd_validate_warning)
                }
                else -> {
                    login(loginIdInputLayout.text.toString(), loginPwdInputLayout.text.toString())
                }
            }
        }

        signUpBtn.setOnClickListener {
            ActivityTransition.startStatic(
                this@LoginActivity,
                SignUpActivity::class.java
            )
        }
    }

    fun warning_about(stringSourceId: Int) {
        loginWarningTextBox.visibility = TextView.VISIBLE
        loginWarningTextBox.text = getString(stringSourceId)
    }

    fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user?.isEmailVerified == false) {
                        warning_about(R.string.email_not_verified_warning)
                        return@addOnCompleteListener
                    }
                    fetchUserData(
                        onSuccess = { userData ->
                            UserManager.setUser(
                                userData.Name,
                                userData.email,
                                userData.image,
                                userData.phoneNumber,
                                userData.point,
                                userData.uid,
                                userData.rankPoint,
                                userData.description
                            )
                            Log.i("LOGIN", "Log In Success. USER INFO: ${userData.Name}, UID: ${userData.uid}")
                            Toast.makeText(this, R.string.login_success, Toast.LENGTH_LONG).show()
                            ActivityTransition.startStatic(
                                this@LoginActivity,
                                HomeAcitivity::class.java
                            )
                        },
                        onFailure = { errorMessage ->
                            Log.e("LOGIN", "Log In failure due to DataSet: $errorMessage")
                        }
                    )
                } else {
                    task.exception?.let {
                        Log.e("LOGIN", "Login failed: ${it.message}")
                    }
                    loginIdInputLayout.requestFocus()
                    warning_about(R.string.login_failure)
                }
            }
    }

    fun fetchUserData(
        onSuccess: (User) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()

        val currentUser = auth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid

            firestore.collection("user").document(uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val user = document.toObject(User::class.java)
                        if (user != null) {
                            user.uid = uid
                            onSuccess(user)
                        } else {
                            onFailure(Exception("사용자 데이터 변환 실패"))
                        }
                    } else {
                        onFailure(Exception("사용자 문서가 존재하지 않습니다"))
                    }
                }
                .addOnFailureListener { exception ->
                    onFailure(exception)
                }
        } else {
            onFailure(Exception("로그인된 사용자가 없습니다"))
        }
    }
}
