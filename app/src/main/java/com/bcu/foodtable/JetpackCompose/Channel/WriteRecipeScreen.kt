package com.bcu.foodtable.JetpackCompose.Channel



import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WriteRecipeScreen(channelName: String) {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category1 by remember { mutableStateOf("") }
    var category2 by remember { mutableStateOf("") }
    val ingredients = remember { mutableStateListOf<String>() }
    val tags = remember { mutableStateListOf<String>() }
    val steps = remember { mutableStateListOf<String>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        RecipeImagePicker(
            selectedImageUri = selectedImageUri,
            onImageSelected = { selectedImageUri = it }
        )

        RecipeTitleInput(
            title = title,
            description = description,
            onTitleChange = { title = it },
            onDescriptionChange = { description = it }
        )

        CategorySelector(
            selectedCategory1 = category1,
            selectedCategory2 = category2,
            onCategory1Change = { category1 = it },
            onCategory2Change = { category2 = it }
        )

        RecipeStageEditor(
            steps = steps,
            onStepAdded = { steps.add(it) }
        )

        IngredientsEditor(
            ingredients = ingredients,
            onAddIngredient = { ingredients.add(it) }
        )

        TagsEditor(
            tags = tags,
            onAddTag = { tags.add(it) }
        )

        RecipeUploadButton(
            selectedImageUri = selectedImageUri,
            title = title,
            description = description,
            categories = listOf(category1, category2),
            tags = tags,
            ingredients = ingredients,
            steps = steps,
            channelName = channelName
        )
    }
}
