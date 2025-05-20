package com.bcu.foodtable.JetpackCompose.Channel

//import android.os.Bundle
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import dagger.hilt.android.AndroidEntryPoint
//
//@AndroidEntryPoint
//class ChannelActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        val channelName = intent.getStringExtra("channel_name") ?: ""
//
//        setContent {
//            YourAppTheme {
//                ChannelScreen(
//                    channelName = channelName,
//                    onRecipeClick = { recipeId ->
//                        val intent = Intent(this, RecipeViewActivity::class.java)
//                        intent.putExtra("recipe_id", recipeId)
//                        startActivity(intent)
//                    },
//                    onWriteClick = { channelName ->
//                        val intent = Intent(this, WriteActivity::class.java)
//                        intent.putExtra("channel_name", channelName)
//                        startActivity(intent)
//                    }
//                )
//            }
//        }
//    }
//}
