package com.bcu.foodtable.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bcu.foodtable.R
import com.bcu.foodtable.databinding.FragmentHomeBinding


class HomeFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var firstAdapter: CategorieAdapter
    private val dataListBig: MutableList<String> = mutableListOf("한식", "양식", "일식", "중식","기타","임시음식")  // 가변형 리스트

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
        // 카테고리 (가로)ㅁ
        recyclerView = binding.RecyclerViewCategories
        firstAdapter = CategorieAdapter(dataListBig)

        setupRecyclerView(recyclerView,firstAdapter,1)



        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun addCategory(category: String,dataList:MutableList<String>) {
        dataList.add(category)  // 카테고리 추가
        firstAdapter.notifyItemInserted(dataList.size - 1)  // 새 아이템을 어댑터에 알림
    }
    private fun setupRecyclerView(recyclerView: RecyclerView, adapter: CategorieAdapter,spanCount:Int = 1) {
        recyclerViewInit(recyclerView, spanCount)
        recyclerView.adapter = adapter
    }

    private fun recyclerViewInit(recyclerView: RecyclerView, spanCount: Int = 1) {
        recyclerView.layoutManager = if (spanCount == 1) {
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        } else {
            GridLayoutManager(requireContext(), spanCount,GridLayoutManager.HORIZONTAL,false)
        }
    }
}

class CategorieAdapter(private val data: MutableList<String>) :
    RecyclerView.Adapter<CategorieAdapter.ButtonViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ButtonViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
        return ButtonViewHolder(view)
    }

    override fun onBindViewHolder(holder: ButtonViewHolder, position: Int) {
        holder.bind(data[position])
    }

    override fun getItemCount(): Int = data.size

    class ButtonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val button: Button = itemView.findViewById(R.id.categoryName)

        fun bind(item: String) {
            button.text = item
            button.setOnClickListener {
                // 버튼 클릭 시 동작
                println("Clicked : ${item}")
            }
        }
    }
}