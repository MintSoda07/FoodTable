package com.bcu.foodtable.ui.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridView
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.bcu.foodtable.databinding.FragmentHomeBinding
import com.bcu.foodtable.RecipeViewActivity
import com.bcu.foodtable.useful.CategoryAdapter
import com.bcu.foodtable.useful.RecipeAdapter
import com.bcu.foodtable.useful.RecipeItem
import com.bcu.foodtable.useful.UsefulRecycler
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var firstAdapter: CategoryAdapter
    private lateinit var cardGridView: GridView
    private lateinit var cardGridAdapter: RecipeAdapter
    private lateinit var viewModel: HomeViewModel

    private val dataListBig: MutableList<String> =
        mutableListOf("한식", "양식", "일식", "중식", "기타")

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("HomeFragment", "onCreateView 호출됨")
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // 카테고리 리사이클러뷰 설정
        recyclerView = binding.RecyclerViewCategories
        viewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        firstAdapter = CategoryAdapter(dataListBig) { item ->
            Log.d("HomeFragment", "카테고리 클릭됨: $item")
        }
        UsefulRecycler.setupRecyclerView(recyclerView, firstAdapter, requireContext(), 1)
        Log.d("HomeFragment", "카테고리 리사이클러뷰 설정 완료")

        // 레시피 그리드뷰 설정
        cardGridView = binding.cardGridView
        cardGridAdapter = RecipeAdapter(requireContext(), mutableListOf())
        cardGridView.adapter = cardGridAdapter
        Log.d("HomeFragment", "GridView 및 어댑터 설정 완료")

        cardGridView.setOnItemClickListener { _, _, position, _ ->
            val clickedRecipe = cardGridAdapter.getItem(position) as? RecipeItem
            clickedRecipe?.let {
                val id = it.id
                Log.d("HomeFragment", "레시피 아이템 클릭됨: $id")
                val intent = Intent(context, RecipeViewActivity::class.java)
                intent.putExtra("recipe_id", id)
                context?.startActivity(intent)
            }
        }

        // 추천 레시피 로딩 시작
        Log.d("HomeFragment", "추천 레시피 불러오기 시작")
        RecommendManager.recommendTopRecipes(limit = 10) { recommendedList ->
            Log.d("HomeFragment", "RecommendManager callback 호출됨, 개수: ${recommendedList.size}")
            if (recommendedList.isEmpty()) {
                Log.d("HomeFragment", "추천 결과 없음 → fallback 으로 조회수 순 가져오기")
                firestore.collection("recipe")
                    .orderBy("clicked", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(10)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        Log.d("HomeFragment", "Fallback 쿼리 성공, 문서 수: ${snapshot.size()}")
                        val fallbackRecipes = snapshot.documents.map {
                            val recipe = it.toRecipeItem()
                            Log.d("HomeFragment", "Fallback 레시피: ${recipe.name}, 클릭수: ${recipe.clicked}")
                            recipe
                        }
                        cardGridAdapter.updateRecipes(fallbackRecipes)
                    }
                    .addOnFailureListener { e ->
                        Log.e("HomeFragment", "Fallback 쿼리 실패", e)
                    }
            } else {
                val recommendedRecipes = recommendedList.map { (doc, score) ->
                    val recipe = doc.toRecipeItem()
                    Log.d("HomeFragment", "추천 레시피: ${recipe.name}, 클릭수: ${recipe.clicked}, 추천 점수: $score")
                    recipe
                }
                cardGridAdapter.updateRecipes(recommendedRecipes)
            }
        }

        return root
    }

    override fun onDestroyView() {
        Log.d("HomeFragment", "onDestroyView 호출됨")
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("HomeFragment", "onViewCreated 호출됨")
    }
}

class HomeViewModel : ViewModel() {
    val recipes = MutableLiveData<MutableList<RecipeItem>>()
}

fun DocumentSnapshot.toRecipeItem(): RecipeItem {
    val item = RecipeItem(
        name = this.getString("name") ?: "",
        description = this.getString("description") ?: "",
        imageResId = this.getString("imageResId") ?: "",
        clicked = (this.getLong("clicked") ?: 0L).toInt(),
        date = this.getTimestamp("date") ?: com.google.firebase.Timestamp.now(),
        order = this.getString("order") ?: "",
        id = this.id,
        C_categories = this.get("c_categories") as? List<String> ?: listOf(),
        note = this.getString("note") ?: "",
        tags = this.get("tags") as? List<String> ?: listOf(),
        ingredients = this.get("ingredients") as? List<String> ?: listOf(),
        contained_channel = this.getString("contained_channel") ?: "",
        estimatedCalories = this.getString("estimatedCalories")
    )
    Log.d("DocumentSnapshot", "toRecipeItem 변환됨: ${item.name}, ID: ${item.id}")
    return item
}
