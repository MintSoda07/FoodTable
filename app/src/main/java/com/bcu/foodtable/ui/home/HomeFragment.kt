package com.bcu.foodtable.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.bcu.foodtable.useful.CategoryAdapter
import com.bcu.foodtable.databinding.FragmentHomeBinding
import com.bcu.foodtable.useful.UsefulRecycler


class HomeFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var firstAdapter: CategoryAdapter


    private val dataListBig: MutableList<String> =
        mutableListOf("한식", "양식", "일식", "중식", "기타", "임시음식")  // 가변형 리스트 (임시)

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

        firstAdapter = CategoryAdapter(
            dataListBig){ item ->
            println("Clicked: $item")
        }
        UsefulRecycler.setupRecyclerView(recyclerView, firstAdapter, requireContext(),1)



        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }



}

