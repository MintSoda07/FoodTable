package com.bcu.foodtable.useful

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.DocumentId

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

    @PropertyName("authorId")
    val authorId: String = "",

    @PropertyName("authorName")
    val authorName: String = "",

    @PropertyName("priceInSalt")
    val priceInSalt: Int = 0,

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
    var likes: Int = 0,

    @PropertyName("likedUsers")
    var likedUsers: List<String> = listOf(),

    @PropertyName("likedUsers")
    var cost: Int = 0,

    @PropertyName("cookingDuration")
    var duration: Int = 0,
)