package com.bcu.foodtable

import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bcu.foodtable.useful.ActivityTransition
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    lateinit var loginIdInputLayout : TextInputEditText
    lateinit var loginPwdInputLayout : TextInputEditText
    lateinit var loginWarningTextBox : TextView
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
        val passwordPattern = "^(?=.*[A-Z])(?=.*[@#\$%^&+=!]).{6,48}\$"

        loginIdInputLayout = findViewById(R.id.idInputBox)
        loginPwdInputLayout = findViewById(R.id.pwdInputBox)

        loginWarningTextBox = findViewById(R.id.warningText)

        submitBtn.setOnClickListener{
            if(loginIdInputLayout.text!!.length <= 0){
              loginIdInputLayout.requestFocus()
                warning_about(R.string.id_empty_warning)
            }else if(loginPwdInputLayout.text!!.length <= 0){
                loginPwdInputLayout.requestFocus()
                warning_about(R.string.pwd_empty_warning)
            }else if(loginPwdInputLayout.text!!.matches(Regex(passwordPattern)) == false){
                loginPwdInputLayout.requestFocus()
                warning_about(R.string.pwd_validate_warning)
            }else{
                login(loginIdInputLayout.text.toString(),loginPwdInputLayout.text.toString())
            }
        }

        signUpBtn.setOnClickListener{
            ActivityTransition.startStatic(
                this@LoginActivity,
                SignUpAcitivity::class.java
            )
        }
    }

    // 경고 텍스트박스에 경고 메시지를 출력하는 함수
    fun warning_about(stringSourceId: Int){
        if(loginWarningTextBox.visibility==TextView.INVISIBLE) loginWarningTextBox.visibility = TextView.VISIBLE
        loginWarningTextBox.text=getString(stringSourceId)
    }

    //
    fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // 로그인 성공
                    Toast.makeText(this, R.string.login_success, Toast.LENGTH_LONG).show()
                    ActivityTransition.startStatic(
                        this@LoginActivity,
                        HomeAcitivity::class.java
                    )
                } else {
                    // 로그인 실패
                    loginIdInputLayout.requestFocus()
                    warning_about(R.string.login_failure)
                }
            }
    }
}