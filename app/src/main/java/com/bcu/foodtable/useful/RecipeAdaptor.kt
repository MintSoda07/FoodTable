package com.bcu.foodtable.useful

import android.content.Context
import android.view.*
import android.widget.*
import com.bcu.foodtable.R

// 레시피 아이템을 뷰에 추가하는 어댑터.
class RecipeAdapter(private val context: Context, private val recipes: List<RecipeItem>) : BaseAdapter() {

    override fun getCount(): Int = recipes.size

    override fun getItem(position: Int): Any = recipes[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView
            ?: LayoutInflater.from(context).inflate(R.layout.cardgrid_recycle, parent, false)

        // View 참조
        val imageView = view.findViewById<ImageView>(R.id.RecipeImage)
        val nameTextView = view.findViewById<TextView>(R.id.RecipeName)
        val descriptionTextView = view.findViewById<TextView>(R.id.RecipeDes)

        // 데이터 설정
        val recipe = recipes[position]
        imageView.setImageResource(recipe.imageResId)
        nameTextView.text = recipe.name
        descriptionTextView.text = recipe.description

        return view
    }
}