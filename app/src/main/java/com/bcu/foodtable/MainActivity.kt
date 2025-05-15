package com.bcu.foodtable

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bcu.foodtable.JetpackCompose.MainLoginScreen
import com.bcu.foodtable.ui.ChallengeActivity
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

                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(48.dp))

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

                            Spacer(modifier = Modifier.height(32.dp))

                            // 챌린지 이동 버튼
                            Button(
                                onClick = {
                                    if (isFloated) {
                                        startActivity(Intent(this@MainActivity, ChallengeActivity::class.java))
                                    }
                                },
                                enabled = isFloated,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text("챌린지", style = MaterialTheme.typography.titleMedium)
                            }

                            Spacer(modifier = Modifier.height(48.dp))
                        }
                    }
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
            .addOnFailureListener { e ->
                Log.e("Firestore", "카테고리 조회 실패", e)
            }

        // 레시피 컬렉션 확인 및 샘플 데이터 추가
        val recipesRef = db.collection("recipe")
        recipesRef.get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.d("Firestore", "레시피 컬렉션이 비어있음. 샘플 데이터 추가 시작")
                    addSampleRecipes()
                } else {
                    Log.d("Firestore", "레시피 데이터 존재: ${documents.size()} 개")
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "레시피 조회 실패", e)
            }
    }

    private fun addSampleRecipes() {
        val sampleRecipes = listOf(
            mapOf(
                "name" to "김치찌개",
                "description" to "매콤하고 얼큰한 전통 한식",
                "imageResId" to "https://firebasestorage.googleapis.com/v0/b/foodtable-a7f2a.appspot.com/o/sample%2Fkimchi_stew.jpg?alt=media",
                "clicked" to 0,
                "date" to com.google.firebase.Timestamp.now(),
                "order" to "1",
                "C_categories" to listOf("한식", "찌개"),
                "tags" to listOf("매운", "얼큰", "김치"),
                "ingredients" to listOf("김치", "돼지고기", "두부"),
                "note" to "김치는 잘 익은 것을 사용하세요"
            ),
            mapOf(
                "name" to "된장찌개",
                "description" to "구수한 우리의 맛",
                "imageResId" to "https://firebasestorage.googleapis.com/v0/b/foodtable-a7f2a.appspot.com/o/sample%2Fsoybean_stew.jpg?alt=media",
                "clicked" to 0,
                "date" to com.google.firebase.Timestamp.now(),
                "order" to "2",
                "C_categories" to listOf("한식", "찌개"),
                "tags" to listOf("구수", "건강"),
                "ingredients" to listOf("된장", "두부", "애호박"),
                "note" to "된장은 집된장이 가장 맛있어요"
            )
        )

        sampleRecipes.forEach { recipe ->
            db.collection("recipe")
                .add(recipe)
                .addOnSuccessListener { documentReference ->
                    Log.d("Firestore", "레시피 추가 성공: ${documentReference.id}")
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "레시피 추가 실패", e)
                }
        }
    }
}
