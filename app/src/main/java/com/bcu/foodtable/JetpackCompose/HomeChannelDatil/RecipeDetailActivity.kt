package com.bcu.foodtable.JetpackCompose.HomeChannelDatil

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.bcu.foodtable.ui.subscribeNavMenu.EditRecipeActivity
import com.bcu.foodtable.useful.FirebaseHelper
import com.bcu.foodtable.useful.RecipeItem
import com.google.firebase.firestore.FirebaseFirestore

class RecipeDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val recipeId = intent.getStringExtra("recipe_id") ?: return

        setContent {
            var recipe by remember { mutableStateOf<RecipeItem?>(null) }

            LaunchedEffect(recipeId) {
                val result = FirebaseHelper.getDocumentById("recipe", recipeId, RecipeItem::class.java)
                recipe = result?.apply { id = recipeId }
            }

            recipe?.let {
                RecipeDetailScreen(
                    recipe = it,
                    onEditClick = {
                        startActivity(Intent(this, EditRecipeActivity::class.java).apply {
                            putExtra("recipe_id", recipeId)
                        })
                    },
                    onDeleteClick = {
                        FirebaseFirestore.getInstance().collection("recipe")
                            .document(recipeId)
                            .delete()
                            .addOnSuccessListener {
                                Toast.makeText(this, "삭제되었습니다.", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                    }
                )
            } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}
