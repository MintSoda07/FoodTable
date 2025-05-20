package com.bcu.foodtable.JetpackCompose.Channel



import android.net.Uri
import android.widget.Toast
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.bcu.foodtable.useful.FireStoreHelper
import com.bcu.foodtable.useful.RecipeItem
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.util.*

@Composable
fun RecipeUploadButton(
    selectedImageUri: Uri?,
    title: String,
    description: String,
    categories: List<String>,
    tags: List<String>,
    ingredients: List<String>,
    steps: List<String>,
    channelName: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isUploading by remember { mutableStateOf(false) }

    Button(
        onClick = {
            if (selectedImageUri == null) {
                Toast.makeText(context, "이미지를 선택해주세요", Toast.LENGTH_SHORT).show()
                return@Button
            }
            if (title.isBlank() || description.isBlank()) {
                Toast.makeText(context, "제목과 설명을 입력해주세요", Toast.LENGTH_SHORT).show()
                return@Button
            }

            isUploading = true

            FireStoreHelper.uploadImage(
                imageUri = selectedImageUri,
                imageName = UUID.randomUUID().toString(),
                folderName = "recipe_image",
                onSuccess = { imageUrl ->
                    val orderString = "○" + steps.joinToString("○")
                    val recipeItem = RecipeItem(
                        name = title,
                        description = description,
                        imageResId = imageUrl,
                        clicked = 0,
                        date = Timestamp.now(),
                        order = orderString,
                        id = "",
                        C_categories = categories,
                        note = "",
                        tags = tags,
                        ingredients = ingredients,
                        contained_channel = channelName
                    )

                    val firestore = FirebaseFirestore.getInstance()
                    val recipeRef = firestore.collection("recipe").document()
                    recipeItem.id = recipeRef.id
                    recipeRef.set(recipeItem)
                        .addOnSuccessListener {
                            Toast.makeText(context, "레시피가 업로드되었습니다!", Toast.LENGTH_SHORT).show()
                            isUploading = false
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "업로드 실패: ${it.message}", Toast.LENGTH_SHORT).show()
                            isUploading = false
                        }
                },
                onFailure = { exception ->
                    Toast.makeText(context, "업로드 실패: ${exception.message}", Toast.LENGTH_SHORT).show()
                    isUploading = false
                }
            )
        },
        enabled = !isUploading
    ) {
        Text(if (isUploading) "업로드 중..." else "레시피 업로드")
    }
}
