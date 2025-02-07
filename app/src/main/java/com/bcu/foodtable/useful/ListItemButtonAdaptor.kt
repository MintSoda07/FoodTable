package com.bcu.foodtable.useful

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bcu.foodtable.R

class ListItemButtonAdaptor(private val items: List<String>,private val onClick: (position: Int) -> Unit) : RecyclerView.Adapter<ListItemButtonAdaptor.ViewHolder>() {
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val NumberView: TextView = view.findViewById(R.id.listItemNumber)
        val TextInnerView: TextView = view.findViewById(R.id.listItemText)
        val ButtonView:Button = view.findViewById(R.id.listItemButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_with_btn, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.NumberView.text = (position + 1).toString() // 순서를 1부터 시작하도록 설정
        holder.TextInnerView.text = item

        holder.ButtonView.setOnClickListener {
            onClick(position)
        }
    }

    override fun getItemCount() = items.size
}