package com.bcu.foodtable.useful

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bcu.foodtable.R

class FlexAdaptor(private val items: MutableList<String>) : RecyclerView.Adapter<FlexAdaptor.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.itemTextInTag)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.flex_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.textView.text = item

        // 텍스트 길이에 따라 아이템의 너비를 자동 조정
        holder.textView.post {
            val textWidth = holder.textView.paint.measureText(item) + 40 // 텍스트 크기에 여백 추가
            holder.textView.layoutParams.width = textWidth.toInt()
            holder.textView.requestLayout()
        }
    }

    override fun getItemCount() = items.size

    // 새로운 아이템 추가
    fun addItem(newItem: String) {
        items.add(newItem)
        notifyItemInserted(items.size - 1)
    }
}
