package com.bcu.foodtable.ui

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bcu.foodtable.R
import com.bcu.foodtable.model.Challenge
import com.bcu.foodtable.model.ChallengeType
import com.bcu.foodtable.useful.UserManager
import com.bcu.foodtable.viewmodel.ChallengeViewModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


@Composable
fun ChallengeScreen(viewModel: ChallengeViewModel) {
    val challenges by viewModel.challenges.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    var user by remember { mutableStateOf(UserManager.getUser()!!) }
    var userSalt by remember { mutableStateOf(user.point) }

    val context = LocalContext.current

    fun updateUserSalt(amount: Int) {
        val updatedUser = user.copy(point = user.point + amount)
        user = updatedUser
        userSalt = updatedUser.point
        UserManager.setUserByDatatype(updatedUser)

        Firebase.firestore
            .collection("user")
            .document(user.uid)
            .update("point", updatedUser.point)
            .addOnFailureListener {
                Log.e("ChallengeScreen", "🔥 Firestore 업데이트 실패: ${it.localizedMessage}")
            }
    }

    when {
        loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("⚠ ${error ?: "오류 발생"}", color = MaterialTheme.colorScheme.error)
        }

        else -> ChallengeScreenContent(
            challenges = challenges,
            salt = userSalt,
            onProgressUpdate = { id, value ->
                viewModel.updateProgress(id, value)
                val reward = challenges.find { it.id == id }?.reward ?: 0
                updateUserSalt(reward)
            },
            onStartChallenge = { id ->
                viewModel.startChallenge(id)
            }
        )
    }
}





