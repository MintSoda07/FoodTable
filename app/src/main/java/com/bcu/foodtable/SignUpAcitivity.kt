package com.bcu.foodtable

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bcu.foodtable.useful.User as UserData
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignUpActivity : AppCompatActivity() {

    private lateinit var emailInput: TextInputEditText
    private lateinit var nicknameInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var confirmPasswordInput: TextInputEditText
    private lateinit var warningText: TextView
    private lateinit var emailVerifyButton: Button
    private lateinit var nicknameCheckButton: Button
    private lateinit var signUpButton: Button

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var isEmailVerified = false
    private var isNicknameValid = false
    private val temporaryPassword = "TemporaryPass123!" // 임시 비밀번호

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
        warningText = findViewById(R.id.warningTextSignUp)
        emailVerifyButton = findViewById(R.id.emailVerifyButton)
        nicknameCheckButton = findViewById(R.id.nicknameCheckButton)
        signUpButton = findViewById(R.id.loginSubmitBtn2)

        emailVerifyButton.setOnClickListener {
            val email = emailInput.text.toString()
            if (email.isNotEmpty()) {
                sendEmailVerification(email)
            } else {
                showWarning("이메일을 입력하세요.")
            }
        }

        nicknameCheckButton.setOnClickListener {
            val nickname = nicknameInput.text.toString()
            if (nickname.isNotEmpty()) {
                checkNicknameAvailability(nickname) // 이메일 인증 여부와 관계없이 호출 가능하도록 변경
            } else {
                showWarning("닉네임을 입력하세요.")
            }
        }


        signUpButton.setOnClickListener {
            val email = emailInput.text.toString()
            val nickname = nicknameInput.text.toString()
            val password = passwordInput.text.toString()
            val confirmPassword = confirmPasswordInput.text.toString()

            when {
                email.isEmpty() -> showWarning("이메일을 입력하세요.")
                nickname.isEmpty() -> showWarning("닉네임을 입력하세요.")
                password.isEmpty() -> showWarning("비밀번호를 입력하세요.")
                confirmPassword.isEmpty() -> showWarning("비밀번호 확인을 입력하세요.")
                password != confirmPassword -> showWarning("비밀번호가 일치하지 않습니다.")
                !isNicknameValid -> showWarning("닉네임 중복 확인을 완료하세요.")
                !isEmailVerified -> showWarning("이메일 인증을 먼저 완료하세요.")
                else -> updateUserPasswordAndRegister(email, password, nickname)
            }
        }
    }

    /**
     * 1️⃣ 이메일 인증 링크 전송 (임시 계정 생성)
     */
    private fun sendEmailVerification(email: String) {
        auth.fetchSignInMethodsForEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val signInMethods = task.result?.signInMethods ?: emptyList()
                    if (signInMethods.isNotEmpty()) {
                        showWarning("이미 사용 중인 이메일입니다.")
                        return@addOnCompleteListener
                    }

                    auth.createUserWithEmailAndPassword(email, temporaryPassword) // 임시 계정 생성
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val user = auth.currentUser
                                user?.sendEmailVerification()
                                    ?.addOnCompleteListener { verificationTask ->
                                        if (verificationTask.isSuccessful) {
                                            Toast.makeText(this, "이메일 인증 링크가 전송되었습니다.", Toast.LENGTH_LONG).show()
                                            Log.i("SIGNUP", "이메일 인증 링크 전송 완료: $email")
                                        } else {
                                            showWarning("이메일 인증 링크 전송 실패")
                                        }
                                    }
                            } else {
                                showWarning("이메일 계정 생성 실패. 이미 등록된 이메일일 수 있습니다.")
                            }
                        }
                } else {
                    showWarning("이메일 확인 중 오류 발생")
                }
            }
    }

    /**
     * 2️⃣ 이메일 인증 여부 확인
     */
    override fun onResume() {
        super.onResume()
        checkEmailVerification()
    }

    private fun checkEmailVerification() {
        val user = auth.currentUser
        user?.reload()?.addOnCompleteListener {
            if (user?.isEmailVerified == true) {
                isEmailVerified = true
                emailVerifyButton.setBackgroundColor(Color.GREEN)
                emailVerifyButton.text = "이메일 인증 완료"
                emailVerifyButton.isEnabled = false
            }
        }
    }

    /**
     * 3️⃣ 닉네임 중복 확인
     */
    private fun checkNicknameAvailability(nickname: String) {
        firestore.collection("user")
            .whereEqualTo("name", nickname)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "사용 가능한 닉네임입니다.", Toast.LENGTH_SHORT).show()
                    isNicknameValid = true
                    nicknameCheckButton.setBackgroundColor(Color.GREEN)
                } else {
                    showWarning("이미 사용 중인 닉네임입니다.")
                    isNicknameValid = false
                }
            }
            .addOnFailureListener {
                showWarning("닉네임 확인 중 오류 발생")
                isNicknameValid = false
            }
    }

    /**
     * 4️⃣ 회원가입 (비밀번호 변경 후 Firestore에 저장)
     */
    private fun updateUserPasswordAndRegister(email: String, newPassword: String, nickname: String) {
        val user = auth.currentUser
        if (user == null) {
            showWarning("이메일 인증 후 다시 시도하세요.")
            return
        }

        val credential = EmailAuthProvider.getCredential(email, temporaryPassword)
        user.reauthenticate(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    user.updatePassword(newPassword)
                        .addOnCompleteListener { passwordTask ->
                            if (passwordTask.isSuccessful) {
                                createUser(email, newPassword, nickname)
                            } else {
                                showWarning("비밀번호 변경 실패")
                            }
                        }
                } else {
                    showWarning("재인증 실패: ${task.exception?.message}")
                }
            }
    }

    private fun createUser(email: String, password: String, nickname: String) {
        val uid = auth.currentUser?.uid ?: return
        val user = UserData(
            name = nickname,
            email = email,
            image = "",
            phoneNumber = "",
            point = 0,
            rankPoint = 0,
            description = ""
        )
        firestore.collection("user").document(uid)
            .set(user)
            .addOnSuccessListener {
                Toast.makeText(this, "회원가입 성공!", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener {
                showWarning("회원가입 실패")
            }
    }

    private fun showWarning(message: String) {
        warningText.visibility = TextView.VISIBLE
        warningText.text = message
    }
}
