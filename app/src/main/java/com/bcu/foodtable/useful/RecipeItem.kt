package com.bcu.foodtable.useful

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class RecipeItem(
    @PropertyName("name")
    val name: String = "",

    @PropertyName("description")
    val description: String = "",

    @PropertyName("imageResId")
    val imageResId: String = "",

    @PropertyName("clicked")
    val clicked: Int = 0,

    @PropertyName("date")
    val date: Timestamp = Timestamp.now(),

    @PropertyName("order")
    val order: String = "",

    @PropertyName("id")
    var id: String = "",

    @PropertyName("C_categories")
    var C_categories: List<String> = listOf(),

    @PropertyName("note")
    var note: String = "",

    @PropertyName("tags")
    var tags: List<String> = listOf(),

    @PropertyName("ingredients")
    var ingredients: List<String> = listOf(),

    @PropertyName("contained_channel")
    val contained_channel: String = "",

    @PropertyName("estimatedCalories")
    var estimatedCalories: String? = null,

    @PropertyName("likes")
    var likes: Int = 0
)
