package com.bcu.foodtable

import android.os.Bundle
import android.util.Log
import android.widget.GridView
import androidx.appcompat.app.AppCompatActivity
import com.bcu.foodtable.useful.RecipeAdapter
import com.bcu.foodtable.useful.RecipeItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class SearchResultActivity : AppCompatActivity() {
    private lateinit var gridView: GridView
    private lateinit var recipeAdapter: RecipeAdapter
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_result)

        gridView = findViewById(R.id.searchResultGridView)

        // 🔥 Adapter 초기화
        recipeAdapter = RecipeAdapter(this, mutableListOf())
        gridView.adapter = recipeAdapter

        // 🔥 검색어 가져오기
        val query: String = intent.getStringExtra("SEARCH_QUERY") ?: ""

        if (query.isNotBlank()) {
            searchRecipes(query)
        } else {
            Log.e("SearchResultActivity", "검색어가 없음")
        }
    }

    private fun searchRecipes(query: String) {
        db.collection("recipe")
            .orderBy("name", Query.Direction.ASCENDING) // 🔥 성능 최적화 (인덱스 활용)
            .startAt(query)
            .endAt(query + "\uf8ff")
            .get()
            .addOnSuccessListener { documents ->
                val recipeList = mutableListOf<RecipeItem>()
                for (document in documents) {
                    val recipe = document.toObject(RecipeItem::class.java)
                    recipeList.add(recipe)
                }
                recipeAdapter.updateRecipes(recipeList) // 🔥 최적화된 UI 업데이트
            }
            .addOnFailureListener { exception ->
                Log.e("FirestoreSearch", "검색 중 오류 발생: ", exception)
            }
    }
}
