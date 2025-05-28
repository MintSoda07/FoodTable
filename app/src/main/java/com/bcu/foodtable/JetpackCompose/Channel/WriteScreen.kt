package com.bcu.foodtable.JetpackCompose.Channel

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.bcu.foodtable.useful.FireStoreHelper
import com.bcu.foodtable.useful.RecipeItem
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel

class WriteViewModel : ViewModel() {
    var title by mutableStateOf("")
    var description by mutableStateOf("")
    var selectedImageUri by mutableStateOf<Uri?>(null)
    var category1 by mutableStateOf("")
    var category2 by mutableStateOf("")
    var tagList by mutableStateOf<List<String>>(emptyList())
    var ingredientsList by mutableStateOf<List<String>>(emptyList())
    var recipeSteps by mutableStateOf<List<String>>(emptyList())
    var note by mutableStateOf("")
    var isMainImageUploaded by mutableStateOf(false)
    fun onTitleChanged(newTitle: String) {
        title = newTitle
    }

    fun onDescriptionChanged(newDescription: String) {
        description = newDescription
    }

    fun getRecipeStepsFormatted(): String {
        return recipeSteps.joinToString(separator = "\n") { "- $it" }
    }

    fun onImageSelected(uri: Uri) {
        selectedImageUri = uri
    }

    fun addTag(tag: String) {
        tagList = tagList + tag
    }

    fun addIngredient(ingredient: String) {
        ingredientsList = ingredientsList + ingredient
    }

    fun addRecipeStep(step: String) {
        recipeSteps = recipeSteps + step
    }

    fun clearStepInputs() {
        // 이후 Compose UI에서 개별적으로 상태를 관리하도록 구성할 수 있음
    }
    var selectedCategory1 by mutableStateOf("")
    var selectedCategory2 by mutableStateOf("")

    val tags: List<String>
        get() = tagList

    val ingredients: List<String>
        get() = ingredientsList
}



@Composable
fun WriteScreen(channelName: String, onUploadSuccess: () -> Unit) {
    val context = LocalContext.current
    val viewModel: WriteViewModel = viewModel()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.onImageSelected(it)
        } ?: Toast.makeText(context, "이미지를 선택하지 않았습니다.", Toast.LENGTH_SHORT).show()
    }




    LazyColumn(modifier = Modifier.padding(16.dp)) {
        item {
            OutlinedTextField(
                value = viewModel.title,
                onValueChange = { viewModel.onTitleChanged(it) },
                label = { Text("제목") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = viewModel.description,
                onValueChange = { viewModel.onDescriptionChanged(it) },
                label = { Text("설명") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = { imagePickerLauncher.launch("image/*") }) {
                Text("이미지 선택")
            }

            viewModel.selectedImageUri?.let {
                Image(
                    painter = rememberAsyncImagePainter(it),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop
                )

            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (viewModel.selectedImageUri == null) {
                        Toast.makeText(context, "이미지를 선택해 주세요.", Toast.LENGTH_SHORT).show()
                    } else {
                        FireStoreHelper.uploadImage(
                            imageUri = viewModel.selectedImageUri!!,
                            imageName = UUID.randomUUID().toString(),
                            folderName = "recipe_image",
                            onSuccess = { imageUrl ->
                                val recipeItem = RecipeItem(
                                    name = viewModel.title,
                                    description = viewModel.description,
                                    imageResId = imageUrl,
                                    clicked = 0,
                                    date = Timestamp.now(),
                                    order = viewModel.getRecipeStepsFormatted(),
                                    id = "",
                                    C_categories = listOf(viewModel.selectedCategory1, viewModel.selectedCategory2),
                                    note = viewModel.note,
                                    tags = viewModel.tags,
                                    ingredients = viewModel.ingredients,
                                    contained_channel = channelName
                                )

                                val firestore = FirebaseFirestore.getInstance()
                                val recipeRef = firestore.collection("recipe").document()
                                recipeItem.id = recipeRef.id

                                recipeRef.set(recipeItem)
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "레시피가 업로드되었습니다!", Toast.LENGTH_SHORT).show()
                                        onUploadSuccess()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(context, "업로드 실패: ${it.message}", Toast.LENGTH_SHORT).show()
                                    }
                            },
                            onFailure = {
                                Toast.makeText(context, "업로드 실패: ${it.message}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("업로드")
            }
        }
    }
}
