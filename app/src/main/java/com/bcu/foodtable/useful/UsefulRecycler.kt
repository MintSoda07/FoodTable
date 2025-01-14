package com.bcu.foodtable.useful

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

object UsefulRecycler {
    fun addCategory(Adapter:CategoryAdapter,category: String, dataList: MutableList<String>) {
        dataList.add(category)  // 카테고리 추가
        Adapter.notifyItemInserted(dataList.size - 1)  // 새 아이템을 어댑터에 알림
    }
    fun setupRecyclerView(
        recyclerView: RecyclerView,
        adapter: CategoryAdapter,
        context: Context,
        spanCount: Int = 1
    ) {
        recyclerViewInit(recyclerView,context, spanCount)
        recyclerView.adapter = adapter
    }
    fun recyclerViewInit(recyclerView: RecyclerView,context: Context, spanCount: Int = 1) {
        recyclerView.layoutManager = if (spanCount == 1) {
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        } else {
            GridLayoutManager(context, spanCount, GridLayoutManager.HORIZONTAL, false)
        }
    }
}