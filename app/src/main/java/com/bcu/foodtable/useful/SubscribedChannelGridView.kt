package com.bcu.foodtable.useful

import android.content.Context
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bcu.foodtable.R
import com.bumptech.glide.Glide

class SubscribedChannelGridView(private val context: Context, private val itemList: List<Channel>) :
    RecyclerView.Adapter<SubscribedChannelGridView.ViewHolder>() {

    // ViewHolder 클래스 정의
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.ChannelName)
        val imageView: ImageView = view.findViewById(R.id.ChannelImage)
    }

    // ViewHolder 생성 (레이아웃 연결)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.round_subscribed_channels, parent, false)
        return ViewHolder(view)
    }

    // 데이터 바인딩
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = itemList[position]
        holder.nameTextView.text = item.name

        // Glide로 이미지 로드
        Glide.with(context)
            .load(item.imageResId) // 이미지 URL 로드
            .centerCrop()
            .override(500, 500)
            .placeholder(R.drawable.baseline_menu_book_24)
            .error(R.drawable.dish_icon)
            .into(holder.imageView)

    }

    // 아이템 개수 반환
    override fun getItemCount() = itemList.size
}