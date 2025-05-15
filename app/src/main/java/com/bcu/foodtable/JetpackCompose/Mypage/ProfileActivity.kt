package com.bcu.foodtable.JetpackCompose.Mypage

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.bcu.foodtable.PuchasePage
import com.bcu.foodtable.ui.myPage.ChannelCreationActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileActivity : ComponentActivity() {

    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProfileScreen(
                user = viewModel.user,
                onEditClick = { viewModel.startEdit() },
                onSaveClick = { viewModel.saveChanges() },
                onCancelClick = { viewModel.cancelEdit() },
                onPickImageClick = { viewModel.pickImageFromGallery(this) },
                onPurchaseClick = { startActivity(Intent(this, PuchasePage::class.java)) },
                onCreateChannelClick = { startActivity(Intent(this, ChannelCreationActivity::class.java)) }
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        viewModel.handleImageResult(requestCode, resultCode, data)
    }
}
