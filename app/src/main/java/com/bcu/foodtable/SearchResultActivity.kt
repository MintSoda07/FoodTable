package com.bcu.foodtable

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.bcu.foodtable.useful.ExpandedGridView
import com.bcu.foodtable.useful.RecipeAdapter
import com.bcu.foodtable.useful.RecipeItem
import com.google.firebase.firestore.FirebaseFirestore

class SearchResultActivity : AppCompatActivity() {
    private lateinit var expandedGridView: ExpandedGridView
    private lateinit var recipeAdapter: RecipeAdapter
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_result)

        //  ExpandedGridView 가져오기
        expandedGridView = findViewById(R.id.cardGridView)

        //  기존 GridView 대신 ExpandedGridView에 Adapter 연결
        recipeAdapter = RecipeAdapter(this, mutableListOf())
        expandedGridView.adapter = recipeAdapter

        //  검색어 가져오기
        val query: String = intent.getStringExtra("SEARCH_QUERY") ?: ""
        if (query.isNotBlank()) {
            searchRecipes(query)
        } else {
            Log.e("SearchResultActivity", "검색어가 없음")
        }
        //  검색 결과 클릭 시 상세 보기로 이동
        expandedGridView.setOnItemClickListener { _, _, position, _ ->
            val clickedRecipe = recipeAdapter.getItem(position) as? RecipeItem
            clickedRecipe?.let {
                val id = it.id
                Log.d("SearchResultActivity", "RecipeClicked: $id")

                val intent = Intent(this, RecipeViewActivity::class.java)
                intent.putExtra("recipe_id", id) // Firestore 문서 ID 전달
                startActivity(intent)
            }
        }
    }

    private fun searchRecipes(query: String) {
        db.collection("recipe")
            .get() //  Firestore에서 모든 레시피 데이터를 가져옴
            .addOnSuccessListener { documents ->
                val recipeList = mutableListOf<RecipeItem>()
                for (document in documents) {
                    val recipe = document.toObject(RecipeItem::class.java)
                    recipe.id = document.id
                    //  `name` 필드를 개별 단어로 분리
                    val words = splitWords(recipe.name)

                    //  검색어(query)가 단어 리스트에 포함되면 결과 리스트에 추가
                    if (words.any { it.contains(query, ignoreCase = true) }) {
                        recipeList.add(recipe)
                    }
                }
                recipeAdapter.updateRecipes(recipeList) //  UI 업데이트
            }
            .addOnFailureListener { exception ->
                Log.e("FirestoreSearch", "검색 중 오류 발생: ", exception)
            }
    }

    private fun splitWords(name: String): List<String> {
        return name.split(" ", "-", "_") //  띄어쓰기, 하이픈(-), 밑줄(_) 기준으로 단어 분리
    }
}
