package com.bcu.foodtable.ui.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.GridView
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.bcu.foodtable.useful.CategoryAdapter
import com.bcu.foodtable.databinding.FragmentHomeBinding
import com.bcu.foodtable.RecipeViewActivity
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

    // 임시로  집어넣은 값
    private val dataListBig: MutableList<String> =
        mutableListOf("한식", "양식", "일식", "중식", "기타")  // 가변형 리스트

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val firestore = FirebaseFirestore.getInstance()
    private val recipesCollection = firestore.collection("recipe")
    private val pageSize = 20 // 한 번에 가져올 데이터 개수
    private var lastDocument: DocumentSnapshot? = null // 마지막으로 가져온 문서의 참조
    private var isLoading = false // 중복 로드를 방지하기 위한 플래그

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root
        // 카테고리 (가로)
        recyclerView = binding.RecyclerViewCategories

        viewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        // 카테고리 아이템의 클릭을 감지하는 기능
        firstAdapter = CategoryAdapter(
            dataListBig){ item ->
            println("Clicked: $item")
        }
        UsefulRecycler.setupRecyclerView(recyclerView, firstAdapter, requireContext(),1)


        // Recipe GridView 설정
        cardGridView = binding.cardGridView
        cardGridAdapter  = RecipeAdapter(requireContext(), mutableListOf()) // 초기 빈 리스트
        cardGridView.adapter = cardGridAdapter

        cardGridAdapter.onClick={
                clickedRecipe ->
            Log.d("HomeFragment","RecipeClicked : ${clickedRecipe.id}")
                val intent = Intent(context, RecipeViewActivity::class.java)
                intent.putExtra("recipe_id", clickedRecipe.id)  // Firestore 문서 ID 전달
            context?.startActivity(intent)  // 새로운 액티비티로 전환
        }

        cardGridView.setOnScrollListener(object : AbsListView.OnScrollListener {
            override fun onScroll(
                view: AbsListView?,
                firstVisibleItem: Int,
                visibleItemCount: Int,
                totalItemCount: Int
            ) {
                // 마지막 아이템까지 스크롤했을 때 추가 데이터 로드
                if (firstVisibleItem + visibleItemCount >= totalItemCount) {
                    loadMoreRecipes(isInitialLoad = false) { newRecipes ->
                        cardGridAdapter.updateRecipes(newRecipes)
                    }
                }
            }

            override fun onScrollStateChanged(view: AbsListView?, scrollState: Int) {}
        })
        viewModel.recipes.observe(viewLifecycleOwner) { recipes ->
            cardGridAdapter.updateRecipes(recipes)
        }

        // 초기 데이터 로드
        loadMoreRecipes(isInitialLoad = true) { newRecipes ->
            cardGridAdapter.updateRecipes(newRecipes)
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    fun loadMoreRecipes(
        isInitialLoad: Boolean,
        onRecipesLoaded: (List<RecipeItem>) -> Unit
    ) {
        if (viewModel.isLoading) return
        viewModel.isLoading = true

        // 쿼리 작성: "clicked" 필드 기준으로 오름차순 정렬하고 "clicked" 값이 40 이상인 데이터만 가져오기
        var query = recipesCollection
            //.whereGreaterThan("clicked", 40) // "clicked" 값이 40보다 큰 데이터만
            .orderBy("clicked") // "clicked" 필드로 정렬
            .limit(pageSize.toLong()) // 한 번에 20개 데이터 로드

        // 추가 로드 시: lastDocument 기준으로 데이터 이어서 로드
        if (!isInitialLoad && viewModel.lastDocument != null) {
            query = query.startAfter(viewModel.lastDocument)
        }

        query.get()
            .addOnSuccessListener { querySnapshot ->
                val newRecipes = querySnapshot.documents.mapNotNull { document ->
                    val recipe = document.toObject(RecipeItem::class.java)
                    recipe?.copy(id = document.id)
                }.toMutableList()

                if (isInitialLoad) {
                    viewModel.recipes.value = newRecipes
                } else {
                    val currentRecipes = viewModel.recipes.value ?: mutableListOf()
                    currentRecipes.addAll(newRecipes)
                    viewModel.recipes.value = currentRecipes
                }

                if (querySnapshot.documents.isNotEmpty()) {
                    viewModel.lastDocument = querySnapshot.documents.last()
                }

                viewModel.isLoading = false
            }
            .addOnFailureListener {
                viewModel.isLoading = false
            }
    }
}

class HomeViewModel : ViewModel() {
    val recipes = MutableLiveData<MutableList<RecipeItem>>()
    var lastDocument: DocumentSnapshot? = null
    var isLoading = false
}