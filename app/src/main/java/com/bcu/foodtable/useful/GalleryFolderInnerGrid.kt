package com.bcu.foodtable.useful

import android.content.Context
import android.util.Log
import android.view.*
import android.widget.*
import com.bcu.foodtable.R
import com.bumptech.glide.Glide

class GalleryFolderInnerGrid(
    private val context: Context,
    var items: List<GalleryItem>,
    private val targetGroupId: String,
    private val onItemClick: (GalleryItem) -> Unit
) : BaseAdapter() {
    private val filteredItems: List<GalleryItem> = items.filter { it.groupId == targetGroupId }
    override fun getCount(): Int = filteredItems.size

    override fun getItem(position: Int): Any = filteredItems[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.gallery_item, parent, false)

        val imageView: ImageView = view.findViewById(R.id.GalleryItemImage)
        val textView: TextView = view.findViewById(R.id.GalleryItemText)

        val item = getItem(position) as GalleryItem

        // 이미지 로드
        Glide.with(context)
            .load(item.image)
            .centerCrop()
            .override(500, 500)
            .placeholder(R.drawable.baseline_menu_book_24)
            .error(R.drawable.dish_icon)
            .into(imageView)

        // 이름 설정
        textView.text = item.name

        // 클릭 리스너 추가 (선택 동작 처리)
        view.setOnClickListener {
            onItemClick(item)
        }

        return view
    }
}