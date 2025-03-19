package com.bcu.foodtable.useful

import android.content.Context
import android.view.*
import android.widget.*
import com.bcu.foodtable.R
import com.bumptech.glide.Glide

class RecipeAdapter(
    private val context: Context,
     var recipes: MutableList<RecipeItem>
) : BaseAdapter() {

    override fun getCount(): Int = recipes.size

    override fun getItem(position: Int): Any = recipes[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val viewHolder: ViewHolder
        val view: View

        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.cardgrid_recycle, parent, false)
            viewHolder = ViewHolder(view)
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = view.tag as ViewHolder
        }

        val recipe = recipes[position]
        viewHolder.nameTextView.text = recipe.name

        //  최적화된 Glide 로딩
        Glide.with(viewHolder.imageView.context)
            .load(recipe.imageResId)
            .centerCrop()
            .placeholder(R.drawable.baseline_menu_book_24)
            .error(R.drawable.dish_icon)
            .into(viewHolder.imageView)

        return view
    }

    //  ViewHolder 패턴 적용 (성능 최적화)
    private class ViewHolder(view: View) {
        val nameTextView: TextView = view.findViewById(R.id.RecipeName)
        val imageView: ImageView = view.findViewById(R.id.RecipeImage)
    }

    // Firestore 검색 결과를 적용하는 함수 (최적화된 UI 업데이트)
    fun updateRecipes(newRecipes: List<RecipeItem>) {
        recipes.clear()
        recipes.addAll(newRecipes)
        notifyDataSetChanged() // ⚡ 최소한의 UI 업데이트
    }
}
