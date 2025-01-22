package com.bcu.foodtable.useful

import android.content.Context
import android.util.Log
import android.view.*
import android.widget.*
import com.bcu.foodtable.R
import com.bcu.foodtable.useful.FireStoreHelper.loadImageFromUrl
import com.bumptech.glide.Glide

// 레시피 아이템을 뷰에 추가하는 어댑터.
class RecipeAdapter(
    private val context: Context,
    private var recipes: List<RecipeItem> // 데이터를 동적으로 변경할 수 있도록 var로 선언
) : BaseAdapter() {
    var onClick: (RecipeItem) -> Unit = {}
    override fun getCount(): Int = recipes.size

    override fun getItem(position: Int): Any = recipes[position]

    override fun getItemId(position: Int): Long = position.toLong()


    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView
            ?: LayoutInflater.from(context).inflate(R.layout.cardgrid_recycle, parent, false)

        val imageView = view.findViewById<ImageView>(R.id.RecipeImage)
        val nameTextView = view.findViewById<TextView>(R.id.RecipeName)

        val recipe = recipes[position]

        view.setOnClickListener {
            onClick(recipe)
        }

        nameTextView.text = recipe.name
        if (imageView.context != null) {
            Glide.with(imageView.context)
                .load(recipe.imageResId) // 매개변수로 전달받은 URL을 그대로 사용
                .centerCrop()
                .override(500,400)
                .placeholder(R.drawable.baseline_menu_book_24) // 로딩 중 표시할 이미지
                .error(R.drawable.dish_icon) // 실패 시 표시할 이미지
                .into(imageView) // ImageView에 로드
        } else {
            Log.e("FirebaseStorage", "Context is null, cannot load image.")
        }
        return view
    }

    // 어댑터 데이터 업데이트 함수
    fun updateRecipes(newRecipes: List<RecipeItem>) {
        recipes = newRecipes
        notifyDataSetChanged()
    }
}