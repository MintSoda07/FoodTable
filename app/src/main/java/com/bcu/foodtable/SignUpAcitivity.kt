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
import com.bcu.foodtable.useful.UserManager
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import com.bcu.foodtable.useful.User as UserData

class SignUpAcitivity : AppCompatActivity() {

    private lateinit var emailInput: TextInputEditText
    private lateinit var nicknameInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var confirmPasswordInput: TextInputEditText
    private lateinit var verificationCodeInput: TextInputEditText
    private lateinit var warningText: TextView
    private var verificationCode: String? = null

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up_acitivity)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        emailInput = findViewById(R.id.idInputBox2)
        nicknameInput = findViewById(R.id.idInputBox3)
        passwordInput = findViewById(R.id.passwordInputBox)
        confirmPasswordInput = findViewById(R.id.confirmPasswordInputBox)
        verificationCodeInput = findViewById(R.id.verificationCodeInputBox)
        warningText = findViewById(R.id.warningTextSignUp)

        val emailVerifyButton = findViewById<Button>(R.id.emailVerifyButton)
        val nicknameCheckButton = findViewById<Button>(R.id.nicknameCheckButton)
        val signUpButton = findViewById<Button>(R.id.loginSubmitBtn2)

        emailVerifyButton.setOnClickListener {
            val email = emailInput.text.toString()
            if (email.isNotEmpty()) {
                sendVerificationCode(email)
            } else {
                showWarning(R.string.email_empty_warning)
            }
        }

        nicknameCheckButton.setOnClickListener {
            val nickname = nicknameInput.text.toString()
            if (nickname.isNotEmpty()) {
                checkNicknameAvailability(nickname)
            } else {
                showWarning(R.string.nickname_empty_warning)
            }
        }

        signUpButton.setOnClickListener {
            val email = emailInput.text.toString()
            val nickname = nicknameInput.text.toString()
            val password = passwordInput.text.toString()
            val confirmPassword = confirmPasswordInput.text.toString()
            val enteredCode = verificationCodeInput.text.toString()

            when {
                email.isEmpty() -> showWarning(R.string.email_empty_warning)
                nickname.isEmpty() -> showWarning(R.string.nickname_empty_warning)
                password.isEmpty() -> showWarning(R.string.password_empty_warning)
                confirmPassword.isEmpty() -> showWarning(R.string.confirm_password_empty_warning)
                password != confirmPassword -> showWarning(R.string.password_mismatch_warning)
                enteredCode != verificationCode -> showWarning(R.string.verification_code_invalid_warning)
                else -> createUser(email, password, nickname)
            }
        }
    }

    private fun sendVerificationCode(email: String) {
        verificationCode = (100000..999999).random().toString() // 랜덤 6자리 숫자 생성
        // 이메일 전송 로직 추가 필요 (Firebase Functions 또는 이메일 API 활용)
        Log.i("SIGNUP", "Verification Code Sent: $verificationCode")
        Toast.makeText(this, R.string.verification_code_sent, Toast.LENGTH_LONG).show()
    }

    private fun checkNicknameAvailability(nickname: String) {
        firestore.collection("user")
            .whereEqualTo("nickname", nickname)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, R.string.nickname_available, Toast.LENGTH_SHORT).show()
                } else {
                    showWarning(R.string.nickname_unavailable_warning)
                }
            }
            .addOnFailureListener {
                showWarning(R.string.nickname_check_failed)
            }
    }

    private fun createUser(email: String, password: String, nickname: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid ?: return@addOnCompleteListener
                    val user = UserData(
                        Name=nickname,
                        email= email,
                        image="",
                        phoneNumber = "",
                        point= 0,
                        rankPoint = 0,
                        description = ""
                    )
                    firestore.collection("user").document(uid)
                        .set(user)
                        .addOnSuccessListener {
                            Toast.makeText(this, R.string.signup_success, Toast.LENGTH_LONG).show()
                            user.uid=uid
                            finish()
                        }
                        .addOnFailureListener {
                            showWarning(R.string.signup_failed)
                        }
                } else {
                    showWarning(R.string.signup_failed)
                }
            }
    }

    private fun showWarning(messageId: Int) {
        warningText.visibility = TextView.VISIBLE
        warningText.text = getString(messageId)
    }
}
