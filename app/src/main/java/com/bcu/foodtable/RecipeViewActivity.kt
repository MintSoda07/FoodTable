package com.bcu.foodtable

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bcu.foodtable.useful.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RecipeViewActivity : AppCompatActivity() {
    private lateinit var recipeId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_recipe_view)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // Intent로 전달된 데이터 받기
        recipeId = intent.getStringExtra("recipe_id") ?: ""
        CoroutineScope(Dispatchers.Main).launch {
            val recipe = FirebaseHelper.getDocumentById("recipe", recipeId, RecipeItem::class.java)
            recipe?.let {
                it.id = recipeId  // Firestore 문서의 ID를 recipe.id에 할당

                // UI 업데이트
                val placeholder_name = findViewById<TextView>(R.id.itemName_recipe)
                val placeholder_description = findViewById<TextView>(R.id.BasicDescription)
                val placeholder_image = findViewById<ImageView>(R.id.itemImageView)
                val inputString = "%1. 밥을 준비합니다. %.2. 밥을 삶습니다. %3. 밥을 먹습니다."

                // %를 기준으로 문자열을 나눔 (
                val items = inputString.split("%").filter { it.isNotBlank() }
                FireStoreHelper.loadImageFromUrl(it.imageResId,placeholder_image)
                placeholder_name.text = it.name
                placeholder_description.text = it.description

            } ?: run {
                Log.d("Recipe", "No recipe found for the provided ID.")
            }
        }

    }
}
