package com.bcu.foodtable.useful

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.bcu.foodtable.R

class CategoryAdapter(private val data: MutableList<String>, private val onItemClick: (String) -> Unit) :

    RecyclerView.Adapter<CategoryAdapter.ButtonViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ButtonViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
        return ButtonViewHolder(view)
    }

    override fun onBindViewHolder(holder: ButtonViewHolder, position: Int) {
        holder.bind(data[position], onItemClick)  // 콜백을 전달
    }

    override fun getItemCount(): Int = data.size

    class ButtonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val button: Button = itemView.findViewById(R.id.categoryName)

        fun bind(item: String, onClick: (String) -> Unit) {
            button.text = item
            button.setOnClickListener {
                onClick(item)  // 외부에서 전달한 콜백 호출
            }
        }
    }
}