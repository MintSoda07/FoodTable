package com.bcu.foodtable.JetpackCompose.Channel


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.bcu.foodtable.FoodTableApplication
import com.bcu.foodtable.di.ChannelRepository
import com.bcu.foodtable.ui.home.FoodTableTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SubscribeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel = SubscribeViewModel(FirebaseFirestore.getInstance(), getCurrentUserId())

        setContent {
            FoodTableTheme {
                SubscribeScreen(
                    viewModel = viewModel,
                    context = this // ✅ context 전달
                )
            }
        }
    }

    private fun getCurrentUserId(): String {
        return FirebaseAuth.getInstance().currentUser?.uid ?: "defaultUserId"
    }
}
