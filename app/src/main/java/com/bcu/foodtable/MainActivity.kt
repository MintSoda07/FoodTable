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
import com.bcu.foodtable.useful.FireStoreHelper.addCategoriesToFirestore
import com.bcu.foodtable.useful.ViewAnimator
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.ktx.firestore

class MainActivity : AppCompatActivity() {
    // Icons by FlatIcon :: https://www.flaticon.com/kr/ //
    // 저작권 표시 ! 지우지 말 것.

    lateinit var titleText: TextView
    lateinit var subTitleText: TextView
    lateinit var loginBtn: Button
    lateinit var signUpBtn: Button

    lateinit var mainBoxLayout: View

    private val db: FirebaseFirestore = Firebase.firestore

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
                    SignUpActivity::class.java
                )
            }
        }
        checkAndAddFirebase()
    }
    // Firestore에서 카테고리 존재 여부 확인
    private fun checkAndAddFirebase() {

        val categoriesRef = db.collection("C_categories")
        categoriesRef.get()
            .addOnSuccessListener { document ->
                if (document.isEmpty) { // 카테고리 없으면 추가
                    addCategoriesToFirestore()
                }
                else {
                    Log.d("Firestore", "이미 카테고리 존재")
                }

            }
            .addOnFailureListener   { e -> Log.e("Firestore", " 카테고리 조회 실패",e)}
    }
}