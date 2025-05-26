package com.bcu.foodtable.JetpackCompose.HomeChannelDatil

import com.bcu.foodtable.useful.RecipeItem

fun generateRecipeHtml(recipe: RecipeItem): String {
    return """
        <html>
        <head><meta charset="UTF-8"></head>
        <body style="padding:20px; font-family:sans-serif;">
        <h1>${recipe.name}</h1>
        <p>${recipe.description}</p>
        <h2>조리 순서</h2>
        <ul>
        ${recipe.order.split("○").filter { it.isNotBlank() }
        .joinToString("\n") { "<li>$it</li>" }}
        </ul>
        </body>
        </html>
    """.trimIndent()
}
