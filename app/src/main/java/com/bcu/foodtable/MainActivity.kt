package com.bcu.foodtable

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bcu.foodtable.useful.ActivityTransition
import com.bcu.foodtable.useful.UserManager
import com.bcu.foodtable.useful.ViewAnimator

class MainActivity : AppCompatActivity() {
    // Icons by FlatIcon :: https://www.flaticon.com/kr/ //
    // 저작권 표시 ! 지우지 말 것.

    lateinit var titleText: TextView
    lateinit var subTitleText: TextView
    lateinit var loginBtn: Button
    lateinit var signUpBtn: Button

    lateinit var mainBoxLayout: View

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.main_activity)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 사용할 레이아웃 초기화
        titleText = findViewById(R.id.titleText)
        subTitleText = findViewById(R.id.subTitleText)

        loginBtn = findViewById(R.id.logInButton)
        signUpBtn = findViewById(R.id.signUpButton)

        mainBoxLayout = findViewById(R.id.mainLoginAskBox)

        loginBtn.alpha = 0f
        signUpBtn.alpha = 0f

        // 테스트용 유저 설정 // 실제 배포 시 다른 작업으로 대체 // data 입력하는 것임.
        UserManager.setUser("관리자","admin@test.com","01012345678",1500,R.drawable.tacco_sample)

        // 조건 변수 선언
        var is_floated = false
        // 레이아웃 애니메이션
        ViewAnimator.moveXPos(titleText, -950f, 0f, 2000, DecelerateInterpolator(2.0f)).start()
        val listenerAnim =
            ViewAnimator.moveXPos(subTitleText, -1250f, 0f, 2500, DecelerateInterpolator(2.0f))
        listenerAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                ViewAnimator.floatAnimation(loginBtn, 2000, DecelerateInterpolator(3.0f)).start()
                ViewAnimator.floatAnimation(signUpBtn, 4000, DecelerateInterpolator(3.0f))
                    .start()
                is_floated = true
            }
        })
        listenerAnim.start()
        loginBtn.setOnClickListener { // 로그인 버튼 클릭 시
            if (is_floated) {
                mainBoxLayout.isEnabled = false
                mainBoxLayout.isClickable = false
                ActivityTransition.startStatic(this@MainActivity, LoginActivity::class.java)
            }
        }
        signUpBtn.setOnClickListener { // 회원가입 버튼 클릭 시
            if (is_floated) {
                ActivityTransition.startStatic(
                    this@MainActivity,
                    SignUpAcitivity::class.java
                )
            }
        }
    }
}