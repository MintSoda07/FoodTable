package com.bcu.foodtable.JetpackCompose.HomeChannelDatil


import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.bcu.foodtable.useful.FirebaseHelper
import com.bcu.foodtable.useful.RecipeItem
import com.google.firebase.firestore.FirebaseFirestore

class RecipeCookingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val recipeId = intent.getStringExtra("recipe_id")
        if (recipeId.isNullOrEmpty()) {
            Toast.makeText(this, "레시피 ID가 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            var recipe by remember { mutableStateOf<RecipeItem?>(null) }

            LaunchedEffect(Unit) {
                val result = FirebaseHelper.getDocumentById("recipe", recipeId, RecipeItem::class.java)
                recipe = result?.apply { id = recipeId }
            }

            recipe?.let {
                RecipeCookingScreen(recipe = it)
            } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}
