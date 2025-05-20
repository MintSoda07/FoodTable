package com.bcu.foodtable.JetpackCompose.Recipe

// PdfUtils.kt


import com.bcu.foodtable.useful.RecipeItem


fun generateHtmlForPdf(recipe: RecipeItem): String {
    val stepsHtml = recipe.order
        .split("○")
        .filter { it.isNotBlank() }
        .mapIndexed { index, step ->
            """
            <div style="margin-bottom: 12px; padding: 10px; border-left: 4px solid #a0522d; background-color: #fffaf4; border-radius: 6px;">
                <div style="font-weight: bold; font-size: 18px; color: #5a3e2b; margin-bottom: 6px;">
                    Step ${index + 1}
                </div>
                <div style="font-size: 15px; color: #333;">$step</div>
            </div>
            """
        }.joinToString("\n")

    val ingredientsHtml = recipe.ingredients.joinToString(separator = "\n") { "<li>$it</li>" }
    val categories = recipe.C_categories.joinToString(", ")
    val tags = recipe.tags.joinToString(", ")
    val noteHtml = if (recipe.note.isNotBlank()) {
        """
        <h3 style="color:#444;">비고</h3>
        <p style="font-size: 15px; color:#333;">${recipe.note}</p>
        """
    } else {
        ""
    }

    return """
    <!DOCTYPE html>
    <html lang="ko">
    <head>
        <meta charset="UTF-8" />
        <title>${recipe.name} - 레시피</title>
        <style>
            body { font-family: 'Noto Serif KR', serif; background-color: #f8f4ec; padding: 30px; color: #333; }
            h1 { color: #a0522d; font-size: 36px; text-align: center; margin-bottom: 10px; }
            h2 { color: #a0522d; margin-top: 30px; border-bottom: 1px solid #ccc; padding-bottom: 6px; }
            img { display: block; margin: 0 auto 20px; max-width: 100%; border-radius: 12px; }
            ul { margin-left: 20px; }
            .section { margin-bottom: 30px; }
            .footer { font-size: 12px; color: #888; text-align: right; margin-top: 50px; }
        </style>
    </head>
    <body>
        <h1>${recipe.name}</h1>
        <div style="text-align: center; color: #666; margin-bottom: 20px;">
            열람수: ${recipe.clicked}회 • 예상 칼로리: ${recipe.estimatedCalories ?: "정보 없음"}
        </div>
        <img src="${recipe.imageResId}" alt="레시피 이미지" />
        
        <div class="section">
            <h2>설명</h2>
            <p>${recipe.description}</p>
        </div>

        <div class="section">
            <h2>조리 순서</h2>
            $stepsHtml
        </div>

        <div class="section">
            <h2>재료</h2>
            <ul>
                $ingredientsHtml
            </ul>
        </div>

        <div class="section">
            <h2>카테고리</h2>
            <p>$categories</p>
        </div>

        <div class="section">
            <h2>태그</h2>
            <p>$tags</p>
        </div>

        $noteHtml

        <div class="footer">
            채널: ${recipe.contained_channel} • 작성일: ${recipe.date.toDate()}
        </div>
    </body>
    </html>
    """.trimIndent()
}
