package com.bcu.foodtable

import com.bcu.foodtable.JetpackCompose.MainLoginScreen
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.bcu.foodtable.useful.ActivityTransition
import com.bcu.foodtable.useful.FireStoreHelper.addCategoriesToFirestore
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {

    private val db: FirebaseFirestore = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)

        checkAndAddFirebase() // Firestore 초기화

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var isFloated by remember { mutableStateOf(false) }

                    // Compose에서 애니메이션이 끝나면 isFloated = true
                    MainLoginScreen(
                        onLoginClick = {
                            if (isFloated) {
                                ActivityTransition.startStatic(this@MainActivity, LoginActivity::class.java)
                            }
                        },
                        onSignUpClick = {
                            if (isFloated) {
                                ActivityTransition.startStatic(this@MainActivity, SignUpActivity::class.java)
                            }
                        },
                        onAnimationsFinished = {
                            isFloated = true
                        }
                    )
                }
            }
        }
    }

    private fun checkAndAddFirebase() {
        val categoriesRef = db.collection("C_categories")
        categoriesRef.get()
            .addOnSuccessListener { document ->
                if (document.isEmpty) {
                    addCategoriesToFirestore()
                } else {
                    Log.d("Firestore", "이미 카테고리 존재")
                }
            }
            .addOnFailureListener { e -> Log.e("Firestore", "카테고리 조회 실패", e) }
    }
}
