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

    override fun getCount(): Int{
        val filteredItems = getSortedItems().filterNot { it.name.isNullOrEmpty() }.size
        return filteredItems
    }

    override fun getItem(position: Int): Any = getSortedItems()[position]

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
                val newGroupView = LayoutInflater.from(context)
                    .inflate(R.layout.gallery_folder, parent, false)
                val folderName: TextView = newGroupView.findViewById(R.id.GalleryItemText)
                Log.d("GalleryGridAdapter", "New Item Group Gathered :$groupId")
                folderName.text = item.groupId

                // groupId에 해당하는 View를 저장
                groupViews[groupId] = newGroupView
                newGroupView
            }

            // group_layout 안의 GridView를 가져옴
            val cards = listOf(
                groupView.findViewById<View>(R.id.innerCard1),
                groupView.findViewById(R.id.innerCard2),
                groupView.findViewById(R.id.innerCard3),
                groupView.findViewById(R.id.innerCard4)
            )
            val images = listOf(
                groupView.findViewById<ImageView>(R.id.innerImage1),
                groupView.findViewById(R.id.innerImage2),
                groupView.findViewById(R.id.innerImage3),
                groupView.findViewById(R.id.innerImage4),
            )


            // 해당 groupId에 해당하는 아이템들
            val itemsInGroup = getItemsByGroupId(groupId)

            // 아이템 개수에 맞게 카드를 보여주고, 남은 카드는 GONE 처리
            for (i in cards.indices) {
                if (i < itemsInGroup.size) {
                    cards[i].visibility = View.VISIBLE
                    val imageView: ImageView = images[i]
                    Glide.with(context)
                        .load(itemsInGroup[i].image)
                        .centerCrop()
                        .into(imageView)
                } else {
                    cards[i].visibility = View.INVISIBLE
                }
            }
            // cards의 개수를 아이템 개수로 제한
            // GridView용 어댑터를 설정하고 groupId에 해당하는 데이터를 전달
            return groupView
        } else {
            // groupId가 없는 일반 아이템
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.gallery_item, parent, false)

            val imageView: ImageView = view.findViewById(R.id.GalleryItemImage)
            val textItemView: TextView = view.findViewById(R.id.GalleryItemText)

            // Glide로 이미지 로드
            Glide.with(context)
                .load(item.image) // 이미지 URL 로드
                .centerCrop()
                .override(500, 500)
                .placeholder(R.drawable.baseline_menu_book_24)
                .error(R.drawable.dish_icon)
                .into(imageView)

            textItemView.text = item.name
            return view
        }
    }

    // groupId별로 필터링된 데이터를 가져오는 함수
    private fun getItemsByGroupId(groupId: String): List<GalleryItem> {
        return items.filter { it.groupId == groupId }
    }

    // getView를 호출하기 전에 데이터를 정렬하
    private fun getSortedItems(): List<GalleryItem> {
        val groupedItems = items.filter { it.groupId.isNotEmpty() }
            .distinctBy { it.groupId } // groupId별로 고유한 첫 번째 항목만 남김
        val ungroupedItems = items.filter { it.groupId.isEmpty() }

        return groupedItems + ungroupedItems
    }

}