package com.bcu.foodtable.JetpackCompose.Channel

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.bcu.foodtable.FoodTableApplication
import com.bcu.foodtable.RecipeViewActivity
import com.bcu.foodtable.WriteRecipeActivity
import com.bcu.foodtable.ui.home.FoodTableTheme
import com.bcu.foodtable.JetpackCompose.Channel.ChannelViewModel



class ChannelActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appContainer = (application as FoodTableApplication).appContainer
        val repository = appContainer.channelRepository

        val channelName = intent.getStringExtra("channelName") ?: return
        val viewModel = ChannelViewModel(repository) // ✅ OK

        setContent {
            FoodTableTheme {
                ChannelScreen(
                    channelName = channelName,
                    viewModel = viewModel,
                    onRecipeClick = { recipeId ->
                        val intent = Intent(this, RecipeViewActivity::class.java)
                        intent.putExtra("recipe_id", recipeId)
                        startActivity(intent)
                    },
                    onWriteClick = { channelName ->
                        val intent = Intent(this, WriteRecipeActivity::class.java)
                        intent.putExtra("channel_name", channelName)
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

