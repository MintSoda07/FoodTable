package com.bcu.foodtable.JetpackCompose.AI

data class AiUiState(
    val userPoint: Int = 0,
    val inputText: String = "",
    val ingredients: List<String> = emptyList(),
    val recipes: List<String> = emptyList(),
    val recipeDetails: List<String> = emptyList(),
    val isSending: Boolean = false,
    val showWarning: Boolean = true,
    val resultText: String = "",
    val reasonText: String = ""
)