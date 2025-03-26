package com.bcu.foodtable

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Button
import com.bcu.foodtable.useful.UserManager
import com.google.firebase.auth.FirebaseAuth

class Setting : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_setting)

        // FirebaseAuth 인스턴스 초기화
        auth = FirebaseAuth.getInstance()

        // 로그아웃 버튼 클릭 리스너
        val btnLogout = findViewById<Button>(R.id.btn_logout)

        val user = UserManager.getUser()!!.name
        val img = UserManager.getUser()!!.image

        btnLogout.setOnClickListener {
            // 현재 사용자가 로그인되어 있는지 확인
            val currentUser = auth.currentUser
            if (currentUser != null) {
                // 사용자가 로그인 되어 있으면 로그아웃 처리
                auth.signOut()
            }

            // 로그인 화면으로 이동
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish() // 현재 Activity 종료
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
