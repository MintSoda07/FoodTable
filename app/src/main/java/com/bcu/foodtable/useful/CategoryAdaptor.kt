package com.bcu.foodtable.useful

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.bcu.foodtable.R

class CategoryAdapter(
    private val data: MutableList<String>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.ButtonViewHolder>() {

    private var selectedFoodType: String? = null
    private var selectedCookingMethod: String? = null
    private var selectedIngredients = mutableSetOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ButtonViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
        return ButtonViewHolder(view)
    }

    override fun onBindViewHolder(holder: ButtonViewHolder, position: Int) {
        holder.bind(data[position], onItemClick)
    }

    override fun getItemCount(): Int = data.size

    //  선택 상태 업데이트
    fun updateSelection(foodType: String?, cookingMethod: String?, ingredients: Set<String>) {
        selectedFoodType = foodType
        selectedCookingMethod = cookingMethod
        selectedIngredients = ingredients.toMutableSet()
        notifyDataSetChanged()  // 변경된 상태 반영
    }

    inner class ButtonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val button: Button = itemView.findViewById(R.id.categoryName)

        fun bind(item: String, onClick: (String) -> Unit) {
            button.text = item

            //  버튼의 선택 상태에 따라 색상 변경
            val isSelected = when {
                selectedFoodType == item -> true
                selectedCookingMethod == item -> true
                selectedIngredients.contains(item) -> true
                else -> false
            }

            button.isSelected = isSelected  //  선택 상태 반영

            if (isSelected) {
                button.setBackgroundColor(Color.parseColor("#FF0000")) // 진한 빨간색 (선택)
                button.setTextColor(Color.WHITE)
            } else {
                button.setBackgroundColor(Color.parseColor("#FFB3B3")) // 연한 빨간색 (기본)
                button.setTextColor(Color.BLACK)
            }

            button.setOnClickListener {
                animateButton(button)  // 버튼 클릭 시 애니메이션 실행
                onClick(item)
            }
        }
    }


    private fun animateButton(view: View) {
        view.animate()
            .scaleX(1.03f)  // 크기를 너무 크게 하지 않음 (1.05f → 1.03f)
            .scaleY(1.03f)
            .setDuration(50)  // 애니메이션 속도 개선 (100ms → 50ms)
            .withEndAction {
                view.animate().scaleX(1f).scaleY(1f).setDuration(50).start()
            }
            .start()
    }


}
