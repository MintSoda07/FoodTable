package com.bcu.foodtable.useful

import android.view.*
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bcu.foodtable.R

class RecipeDetailRecyclerAdaptor(private var items: MutableList<String>): RecyclerView.Adapter<RecipeDetailRecyclerAdaptor.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_layout, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = items[position]
        holder.textView.text = item
    }

    override fun getItemCount(): Int {
        return items.size
    }

    // 아이템 리스트를 업데이트하는 메서드
    fun updateItems(newItems: List<String>) {
        items.clear() // 기존 리스트 클리어
        items.addAll(newItems) // 새 아이템 추가
        notifyDataSetChanged() // 어댑터에 변경 사항 알리기
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.textView)
    }
}