package com.bcu.foodtable.useful

import android.content.Context
import android.view.*
import android.widget.*
import com.bcu.foodtable.R
import com.bumptech.glide.Glide

class GalleryGroupItemAdapter(
    private val context: Context,
    private val items: List<GalleryItem>
) : BaseAdapter() {
    override fun getCount() = items.size
    override fun getItem(position: Int) = items[position]
    override fun getItemId(position: Int) = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val item = getItem(position)
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.gallery_folder_inside, parent, false)

        val imageView: ImageView = view.findViewById(R.id.GalleryInnerImage)
        Glide.with(imageView.context)
            .load(item.image) // 매개변수로 전달받은 URL을 그대로 사용
            .centerCrop()
            .override(500,400)
            .placeholder(R.drawable.baseline_menu_book_24) // 로딩 중 표시할 이미지
            .error(R.drawable.dish_icon) // 실패 시 표시할 이미지
            .into(imageView) // ImageView에 로드
        notifyDataSetChanged()
        return view
    }
}