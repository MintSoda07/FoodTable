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
        val tagList: List<String> = intent.getStringArrayListExtra("TAG_LIST") ?: emptyList()
        searchRecipesWithFilters(query, tagList)

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

    private fun searchRecipesWithFilters(query: String?, tags: List<String>) {

        if (query.isNullOrBlank()) {
            Log.d("SearchResultActivity", "검색어가 비어 있어서 검색 중단됨")
            return
        }

        db.collection("recipe")
            .get()
            .addOnSuccessListener { documents ->
                val filteredRecipes = mutableListOf<RecipeItem>()
                for (document in documents) {
                    val recipe = document.toObject(RecipeItem::class.java)
                    recipe.id = document.id

                    val categories = document.get("C_categories") as? List<String> ?: emptyList()
                    val nameMatches = query.isNullOrBlank() || recipe.name.contains(query, ignoreCase = true)
                    val tagsMatch = tags.isEmpty() || tags.all { tag ->
                        categories.any { it.contains(tag, ignoreCase = true) }
                    }

                    if ((query.isNullOrBlank() && tagsMatch) || (nameMatches && tagsMatch)) {
                        filteredRecipes.add(recipe)
                    }
                }
                recipeAdapter.updateRecipes(filteredRecipes)
            }
            .addOnFailureListener {
                Log.e("FirestoreSearch", "검색 실패: ${it.message}")
            }
    }

    private fun splitWords(name: String): List<String> {
        return name.split(" ", "-", "_") //  띄어쓰기, 하이픈(-), 밑줄(_) 기준으로 단어 분리
    }
}
