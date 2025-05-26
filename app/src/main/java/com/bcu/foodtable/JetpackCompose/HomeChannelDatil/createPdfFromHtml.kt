package com.bcu.foodtable.JetpackCompose.HomeChannelDatil

import com.bcu.foodtable.useful.RecipeItem
fun generateRecipeHtml(recipe: RecipeItem): String {
    val steps = recipe.order
        .split("○")
        .filter { it.isNotBlank() }
        .mapIndexed { index, step ->
            "<div class='step'><span class='step-number'>STEP ${index + 1}</span><p class='step-text'>${step.trim()}</p></div>"
        }
        .joinToString("\n")

    val ingredients = recipe.ingredients
        .filter { it.isNotBlank() }
        .joinToString("") { "<li>$it</li>" }

    val tagsHtml = recipe.tags.joinToString("") { "<span class='tag'>#${it}</span>" }
    val categoriesHtml = recipe.C_categories.joinToString("") { "<span class='tag'>${it}</span>" }

    return """
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>${recipe.name}</title>
    <style>
        @page {
            size: A4;
            margin: 0;
        }

        body {
            margin: 0;
            font-family: 'Georgia', serif;
            color: #2c3e50;
        }

        .page {
            page-break-after: always;
            height: 100vh;
            box-sizing: border-box;
            padding: 0;
        }

        .cover {
            height: 100vh;
            position: relative;
            overflow: hidden;
        }

        .cover img {
            width: 100%;
            height: 100%;
            object-fit: cover;
            filter: brightness(0.7);
        }

        .cover-overlay {
            position: absolute;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            padding: 40px;
            box-sizing: border-box;
            display: flex;
            flex-direction: column;
            justify-content: flex-end;
            color: white;
        }

        .cover-title {
            font-size: 48px;
            font-weight: bold;
            text-shadow: 2px 2px 6px rgba(0,0,0,0.6);
            margin-bottom: 10px;
        }

        .meta-box {
            background: rgba(255, 255, 255, 0.95);
            color: #333;
            padding: 24px;
            border-radius: 16px;
            box-shadow: 0 4px 16px rgba(0,0,0,0.2);
            max-width: 600px;
            margin-top: 30px;
        }

        .meta-box .row {
            margin-bottom: 10px;
            font-size: 16px;
        }

        .meta-box .label {
            font-weight: bold;
            margin-right: 10px;
            color: #00796B;
        }

        .tag {
            display: inline-block;
            background: #e0f7fa;
            color: #00796B;
            padding: 4px 8px;
            margin: 4px;
            border-radius: 8px;
            font-size: 13px;
        }

        h2 {
            font-size: 26px;
            margin-bottom: 16px;
            border-left: 6px solid #4CAF50;
            padding-left: 12px;
            color: #444;
        }

        ul {
            padding-left: 24px;
            font-size: 16px;
            line-height: 1.8;
            margin: 0;
        }

        .step {
            margin-bottom: 30px;
        }

        .step-number {
            font-size: 20px;
            font-weight: bold;
            color: #388e3c;
        }

        .step-text {
            margin-top: 8px;
            font-size: 16px;
            line-height: 1.6;
            color: #333;
        }
    </style>
</head>
<body>

<!-- 첫 페이지: 이미지 + 정보 카드 -->
<div class="page cover">
    <img src="${recipe.imageResId}" alt="커버 이미지" />
    <div class="cover-overlay">
        <div class="cover-title">${recipe.name}</div>
        <div class="meta-box">
            <div class="row"><span class="label">작성자:</span> ${recipe.authorName}</div>
            <div class="row"><span class="label">칼로리:</span> ${recipe.estimatedCalories ?: "정보 없음"} kcal</div>
            <div class="row"><span class="label">가격:</span> ${recipe.priceInSalt} 소금</div>
            <div class="row"><span class="label">태그:</span> $tagsHtml</div>
            <div class="row"><span class="label">카테고리:</span> $categoriesHtml</div>
        </div>
    </div>
</div>

<!-- 두 번째 페이지: 재료 -->
<div class="page">
    <div style="padding: 40px;">
        <h2>재료 목록</h2>
        <ul>
            $ingredients
        </ul>
    </div>
</div>

<!-- 세 번째 페이지: 조리 순서 -->
<div class="page">
    <div style="padding: 40px;">
        <h2>조리 순서</h2>
        $steps
    </div>
</div>

<!-- 네 번째 페이지 (옵션): 비고 -->
${if (recipe.note.isNotBlank()) """
<div class="page">
    <div style="padding: 40px;">
        <h2>비고</h2>
        <p>${recipe.note}</p>
    </div>
</div>
""" else ""}
</body>
</html>
""".trimIndent()
}
