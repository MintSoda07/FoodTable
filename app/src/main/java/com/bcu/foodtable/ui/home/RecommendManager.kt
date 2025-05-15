package com.bcu.foodtable.ui.home

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

// Firebase 기반 추천 시스템 (Kotlin 기반 예시)
// 필요한 Firebase 라이브러리: Firestore, Firebase Auth

object RecommendManager {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private const val HISTORY_PATH = "history_recipe"
    private const val RECIPE_PATH = "recipe"

    // 1. 레시피 열람 시 히스토리 기록
    fun recordRecipeHistory(recipeId: String, categories: List<String>) {
        val uid = auth.currentUser?.uid ?: return
        val historyRef = firestore.collection(HISTORY_PATH).document(uid)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(historyRef)

            // history 유지 (중복 제거 + 최대 50개)
            val updatedHistory = snapshot.get("history") as? MutableList<String> ?: mutableListOf()
            if (!updatedHistory.contains(recipeId)) updatedHistory.add(0, recipeId)
            if (updatedHistory.size > 50) updatedHistory.subList(50, updatedHistory.size).clear()

            // history_category 는 중복 허용 큐, 최대 100개
            val updatedCategory = snapshot.get("history_category") as? MutableList<String> ?: mutableListOf()
            for (cat in categories) {
                updatedCategory.add(cat)
                if (updatedCategory.size > 100) {
                    updatedCategory.removeAt(0)  // 가장 오래된 항목 제거 (큐처럼)
                }
            }

            transaction.update(historyRef, mapOf(
                "history" to updatedHistory,
                "history_category" to updatedCategory
            ))
        }.addOnSuccessListener {
            // transaction 성공 후 favorite 자동 갱신 실행
            updateFavoriteCategories(uid)
        }
    }
    // 2. 추천 결과 출력 함수: 상위 N개 결과
    fun recommendTopRecipes(
        limit: Int = 15,
        callback: (List<Pair<DocumentSnapshot, Long>>) -> Unit
    )
    {
        val uid = auth.currentUser?.uid ?: return
        val historyRef = firestore.collection(HISTORY_PATH).document(uid)

        historyRef.get().addOnSuccessListener { historySnap ->
            val tags = historySnap.get("history_category") as? List<String> ?: emptyList()
            val weight = historySnap.get("weight") as? List<Long> ?: listOf(10L, 5L, 3L)

            // 상위 3개 태그 기준 가중치 적용
            val categoryMap = tags.take(3).mapIndexed { index, tag ->
                tag to (weight.getOrNull(index) ?: 1L)
            }.toMap()

            firestore.collection(RECIPE_PATH).get().addOnSuccessListener { recipes ->

                val scored = recipes.documents.mapNotNull { doc ->
                    val clicked = doc.getLong("clicked") ?: 0L
                    val cat1 = doc.get("c_categories.0") as? String ?: ""
                    val cat2 = doc.get("c_categories.1") as? String ?: ""

                    val score = clicked * (
                            (categoryMap[cat1] ?: 0L) + (categoryMap[cat2] ?: 0L)
                            )

                    if (score > 0) Pair(doc, score) else null
                }

                // 만약 점수 있는 레시피가 없으면 → 조회수 기반 기본 추천
                val result = if (scored.isEmpty()) {
                    recipes.documents
                        .sortedByDescending { it.getLong("clicked") ?: 0L }
                        .take(limit)
                        .map { it to (it.getLong("clicked") ?: 0L) }  // fallback도 점수 포함
                } else {
                    scored.sortedByDescending { it.second }
                        .take(limit)
                }

                callback(result)
            }
        }
    }


    // 3. 전체 추천 점수 디버깅 출력
    fun debugRecommendationScores(callback: (List<Pair<String, Long>>) -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        val historyRef = firestore.collection(HISTORY_PATH).document(uid)

        historyRef.get().addOnSuccessListener { historySnap ->
            val tags = historySnap.get("history_category") as? List<String> ?: emptyList()
            val weight = historySnap.get("weight") as? List<Long> ?: listOf(10L, 5L, 3L)

            val categoryMap = tags.take(3).mapIndexed { index, tag ->
                tag to (weight.getOrNull(index) ?: 1L)
            }.toMap()

            // 로그 출력: 사용자의 추천 기준 태그
            android.util.Log.d("Recommendation", "[사용자 히스토리 기반 추천]")
            android.util.Log.d("Recommendation", "우선순위 태그 목록:")
            categoryMap.forEach { (tag, w) ->
                android.util.Log.d("Recommendation", "- $tag (가중치: $w)")
            }

            firestore.collection(RECIPE_PATH).get().addOnSuccessListener { recipes ->
                val result = recipes.documents.map { doc ->
                    val id = doc.id
                    val name = doc.getString("name") ?: "(이름 없음)"
                    val clicked = doc.getLong("clicked") ?: 0L
                    val c_categories = doc.get("c_categories") as? List<String> ?: listOf()

                    var score = 0L
                    val matchLog = mutableListOf<String>()

                    c_categories.forEachIndexed { idx, cat ->
                        val catWeight = categoryMap[cat] ?: 0L
                        if (catWeight > 0) {
                            score += clicked * catWeight
                            matchLog.add("카테고리[$idx] '$cat' * 클릭수($clicked) * 가중치($catWeight)")
                        }
                    }

                    // 디버그 출력
                    if (score > 0) {
                        android.util.Log.d("Recommendation", " 추천 대상 레시피: $name ($id)")
                        matchLog.forEach {
                            android.util.Log.d("Recommendation", "  - $it")
                        }
                        android.util.Log.d("Recommendation", "  ▶ 최종 점수: $score\n")
                    }

                    id to score
                }.sortedByDescending { it.second }

                callback(result)
            }
        }
    }

    private fun updateFavoriteCategories(uid: String) {
        val historyRef = firestore.collection(HISTORY_PATH).document(uid)
        historyRef.get().addOnSuccessListener { snapshot ->
            val categories = snapshot.get("history_category") as? List<String> ?: emptyList()

            // 빈도수 계산
            val frequencyMap = categories.groupingBy { it }.eachCount()

            // 최빈값 순서대로 최대 3개 추출
            val topFavorites = frequencyMap.entries
                .sortedByDescending { it.value }
                .take(3)
                .map { it.key }

            // firestore에 favorite 필드 업데이트
            historyRef.update("favorite", topFavorites)
                .addOnSuccessListener {
                    android.util.Log.d("RecommendManager", "favorite 필드 갱신 완료: $topFavorites")
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("RecommendManager", "favorite 필드 갱신 실패", e)
                }
        }
    }

}
