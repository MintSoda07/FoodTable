package com.bcu.foodtable.useful

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bcu.foodtable.R

class ListItemButtonAdaptor(private val items: List<String>,private val items2: List<String>,private val onClick: (position: Int) -> Unit,private val onBtnClick: (position: Int) -> Unit) : RecyclerView.Adapter<ListItemButtonAdaptor.ViewHolder>() {
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val NumberView: TextView = view.findViewById(R.id.listItemNumber)
        val TextInnerView: TextView = view.findViewById(R.id.listItemText)
        val ButtonView:Button = view.findViewById(R.id.listItemButton)
        val CardView: CardView = view.findViewById(R.id.theListCard)
        val ListItemAddView : View = view.findViewById(R.id.listItemAddView)
        val itemTextTag :TextView = view.findViewById(R.id.itemTextTag)
        val ButtonExpanded:Button = view.findViewById(R.id.ListItemSeeMore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_with_btn, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val item2 = items2[position]
        holder.NumberView.text = (position + 1).toString() // 순서를 1부터 시작하도록 설정
        holder.TextInnerView.text = item
        holder.itemTextTag.text = item2
        var statusOpen = false
        holder.CardView.setOnClickListener {
            onClick(position)
            if(!statusOpen){
                statusOpen = true
                holder.ListItemAddView.visibility=View.VISIBLE
            }else{
                statusOpen = false
                holder.ListItemAddView.visibility=View.GONE
            }
        }

        holder.ButtonExpanded.setOnClickListener(){
            onBtnClick(position)
        }
    }

    override fun getItemCount() = items.size
}