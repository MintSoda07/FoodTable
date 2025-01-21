package com.bcu.foodtable.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.bcu.foodtable.R
import com.bcu.foodtable.useful.CategoryAdapter
import com.bcu.foodtable.databinding.FragmentHomeBinding
import com.bcu.foodtable.useful.RecipeAdapter
import com.bcu.foodtable.useful.RecipeItem
import com.bcu.foodtable.useful.UsefulRecycler


class HomeFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var firstAdapter: CategoryAdapter
    private lateinit var CardGridView: GridView


    // 임시로  집어넣은 값
    private val dataListBig: MutableList<String> =
        mutableListOf("한식", "양식", "일식", "중식", "기타")  // 가변형 리스트

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root
        // 카테고리 (가로)
        recyclerView = binding.RecyclerViewCategories

        // 아이템의 클릭을 감지하는 기능
        firstAdapter = CategoryAdapter(
            dataListBig){ item ->
            println("Clicked: $item")
        }
        UsefulRecycler.setupRecyclerView(recyclerView, firstAdapter, requireContext(),1)

        // 임시로 넣어놓은 데이터 2
        val recipes = listOf(
            RecipeItem("3분 카레", "밥과 함께 먹기 좋은 간단한 카레입니다.", R.drawable.curry_sample),
            RecipeItem("봄철 라이스 롤", "향긋한 봄나물이 들어간, 한입에 먹기 좋은 요리입니다.", R.drawable.riceroll_sample),
            RecipeItem(
                "얼큰 칼국수 라멘",
                "굵직한 칼국수 면과 함께 얼큰하고 고소한 국물로 해장을 책임지는 라멘입니다.",
                R.drawable.ramen_sample
            ),
            RecipeItem("바질페스토 피자", "고소한 치즈와 향긋한 바질이 올라간 가정식 피자입니다.", R.drawable.pizza_sample),
            RecipeItem("불고기 완자", "완자로 만들어 먹기 좋은 불고기 요리입니다.", R.drawable.bulgogi_sample),
            RecipeItem(
                "멕시칸 타코",
                "간단하게 또띠아와 함께 마음대로 만들어 먹는 멕시코의 전통 음식, 타코입니다.",
                R.drawable.tacco_sample
            ),
            RecipeItem("고추짬뽕", "매콤한 고추를 우려 만든 국물과 강렬한 불맛이 일품인 짬뽕입니다.", R.drawable.noodle_sample)
        )
        val cardGridAdapter = RecipeAdapter(requireContext(), recipes)

        CardGridView = binding.cardGridView
        CardGridView.adapter = cardGridAdapter

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }



}

