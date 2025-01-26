package com.bcu.foodtable.useful

import android.content.Context
import android.util.Log
import android.view.*
import android.widget.*
import com.bcu.foodtable.R
import com.bumptech.glide.Glide

class GalleryGridAdapter(
    private val context: Context,
    private val items: List<GalleryItem>
) : BaseAdapter() {
    private val groupViews = mutableMapOf<String, View>() // groupId 별 View

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): Any = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val item = getItem(position) as GalleryItem
        val groupId = item.groupId

        if (context == null) {
            Log.e("GalleryGridAdapter", "Context is null!")
            return View(context)  // 기본 빈 뷰 반환
        }


        // groupId가 존재하면 group_layout 또는 기존에 생성된 View를 사용
        if (groupId.isNotEmpty()) {
            val groupView = groupViews[groupId] ?: run {
                // 처음 보는 groupId일 경우 새로운 group_layout 인플레이트
                val newGroupView = LayoutInflater.from(context)
                    .inflate(R.layout.gallery_folder, parent, false)
                val folderName :TextView = newGroupView.findViewById(R.id.GalleryItemText)

                folderName.text=item.groupId

                // groupId에 해당하는 View를 저장
                groupViews[groupId] = newGroupView
                newGroupView
            }
            // group_layout 안의 GridView를 가져옴
            val gridView: GridView = groupView.findViewById(R.id.GalleryInnerItemGrid)
//            gridView.isEnabled = false // 스크롤을 끄기
//            gridView.setOverScrollMode(View.OVER_SCROLL_NEVER) // 오버 스크롤 비활성화

            // GridView용 어댑터를 설정하고 groupId에 해당하는 데이터를 전달
            Log.d("GalleryGridAdapter","Item Group Gathered :${getItemsByGroupId(groupId)}")
            val adapter = GalleryGroupItemAdapter(context, getItemsByGroupId(groupId))
            gridView.adapter = adapter
            notifyDataSetChanged()

            return groupView
        } else {
            Log.d("GalleryGridAdapter","No Group Gathered :${getItemsByGroupId(groupId)}")
            // groupId가 없는 일반 아이템
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.gallery_item, parent, false)

            val imageView: ImageView = view.findViewById(R.id.GalleryItemImage)
            val textView: TextView = view.findViewById(R.id.GalleryItemText)

                Glide.with(context)
                    .load(item.image) // 매개변수로 전달받은 URL을 그대로 사용
                    .centerCrop()
                    .override(500, 500)
                    .placeholder(R.drawable.baseline_menu_book_24) // 로딩 중 표시할 이미지
                    .error(R.drawable.dish_icon) // 실패 시 표시할 이미지
                    .into(imageView) // ImageView에 로드
                textView.text = item.name
            notifyDataSetChanged()
            return view
        }
    }
    // groupId별로 필터링된 데이터를 가져오는 함수
    private fun getItemsByGroupId(groupId: String): List<GalleryItem> {
        return items.filter { it.groupId == groupId }
    }
}