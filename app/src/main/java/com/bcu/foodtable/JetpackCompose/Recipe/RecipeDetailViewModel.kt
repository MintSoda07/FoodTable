package com.bcu.foodtable.JetpackCompose.Recipe

import RecipeItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bcu.foodtable.RecipeViewActivity
import com.bcu.foodtable.useful.UserManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import perfetto.protos.AndroidStartupMetric
import javax.inject.Inject



@HiltViewModel
class RecipeDetailViewModel @Inject constructor(
    private val db: FirebaseFirestore
) : ViewModel() {

    private val _recipe = MutableStateFlow<RecipeItem?>(null)
    val recipe: StateFlow<RecipeItem?> = _recipe

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments

    private val _likes = MutableStateFlow(0)
    val likes: StateFlow<Int> = _likes

    private val _userHasLiked = MutableStateFlow(false)
    val userHasLiked: StateFlow<Boolean> = _userHasLiked

    private val _isOwner = MutableStateFlow(false)
    val isOwner: StateFlow<Boolean> = _isOwner

    private var currentUserId: String = ""
    private var recipeId: String = ""

    fun initialize(recipeId: String, currentUserId: String) {
        this.recipeId = recipeId
        this.currentUserId = currentUserId

        loadRecipe()
        loadComments()
        checkOwner()
        observeLikes()
    }

    private fun loadRecipe() {
        viewModelScope.launch {
            val snapshot = db.collection("recipe").document(recipeId).get().await()
            snapshot.toObject(RecipeItem::class.java)?.let {
                it.id = recipeId
                _recipe.value = it
                _likes.value = snapshot.getLong("likes")?.toInt() ?: 0
                val likedUsers = snapshot.get("likedUsers") as? List<String> ?: emptyList()
                _userHasLiked.value = likedUsers.contains(currentUserId)
            }
        }
    }

    private fun observeLikes() {
        db.collection("recipe").document(recipeId)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    _likes.value = it.getLong("likes")?.toInt() ?: 0
                    val likedUsers = it.get("likedUsers") as? List<String> ?: emptyList()
                    _userHasLiked.value = likedUsers.contains(currentUserId)
                }
            }
    }

    fun toggleLike() {
        viewModelScope.launch {
            val docRef = db.collection("recipe").document(recipeId)
            val snapshot = docRef.get().await()
            val likedUsers = snapshot.get("likedUsers") as? List<String> ?: listOf()
            val likes = snapshot.getLong("likes") ?: 0

            val isLiked = likedUsers.contains(currentUserId)

            val newLikes = if (isLiked) likes - 1 else likes + 1
            val newLikedUsers = if (isLiked)
                likedUsers.filter { it != currentUserId }
            else
                likedUsers + currentUserId

            docRef.update(
                mapOf(
                    "likes" to newLikes.coerceAtLeast(0),
                    "likedUsers" to newLikedUsers
                )
            )
        }
    }

    private fun checkOwner() {
        viewModelScope.launch {
            val recipeDoc = db.collection("recipe").document(recipeId).get().await()
            val channelName = recipeDoc.getString("contained_channel") ?: return@launch
            val channelSnapshot = db.collection("channel")
                .whereEqualTo("name", channelName)
                .get()
                .await()
            val owner = channelSnapshot.documents.firstOrNull()?.getString("owner")
            _isOwner.value = owner == currentUserId
        }
    }

    private fun loadComments() {
        db.collection("recipe").document(recipeId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.toObjects(Comment::class.java) ?: emptyList()
                _comments.value = list
            }
    }

    fun sendComment(commentText: String) {
        val user = UserManager.getUser() ?: return

        val comment = RecipeViewActivity.Comment(
            text = commentText,
            timestamp = System.currentTimeMillis(),
            userId = user.uid,
            userName = user.name,
            userProfileImage = user.image
        )

        db.collection("recipe").document(recipeId)
            .collection("comments")
            .add(comment)
    }
}
