package com.bcu.foodtable.JetpackCompose.Mypage.Setting.MyRecipe

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.bcu.foodtable.ui.theme.WarmLightColorScheme

class MyRecipesActivity : ComponentActivity() {
    private val vm: MyRecipesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = WarmLightColorScheme) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    MyRecipesScreen(
                        viewModel = vm,
                        onOpenRecipe = { recipeId ->
                            val intent = Intent(
                                this,
                                com.bcu.foodtable.JetpackCompose.HomeChannelDatil
                                    .RecipeCookingActivity::class.java
                            ).apply {
                                putExtra("recipe_id", recipeId)
                            }
                            startActivity(intent)
                        },
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}
