package com.bcu.foodtable.ui.myRecipeStorage

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bcu.foodtable.databinding.FragmentRecipeStorageBinding
import com.bcu.foodtable.useful.GalleryGridAdapter
import com.bcu.foodtable.useful.GalleryItem
import com.bcu.foodtable.useful.RecipeItem
import com.bcu.foodtable.useful.User
import com.bcu.foodtable.useful.UserManager
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class RecipeStorageFragment : Fragment() {

    private var _binding: FragmentRecipeStorageBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var gridView: GridView
    private lateinit var adapter: GalleryGridAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecipeStorageBinding.inflate(inflater, container, false)
        val root: View = binding.root




        // Firestore 데이터 가져오기
        fetchGalleryItems(
            onSuccess = { galleryItems ->
                // 어댑터 설정
                Log.d("FB_Gallery","Item List : $galleryItems")
                gridView = binding.GalleryItemsGroup
                adapter = GalleryGridAdapter(requireContext(), galleryItems)
                gridView.adapter = adapter


                // 아이템 개수 기반으로 GridView의 높이 설정
                val numColumns = 2 // GridView의 열(column) 개수 설정
                setGridViewHeightBasedOnItems(gridView, adapter, numColumns)
            },
            onFailure = { exception ->
                Toast.makeText(requireContext(), "Failed to load data: ${exception.message}", Toast.LENGTH_LONG).show()
            }
        )



        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun fetchGalleryItems(onSuccess: (List<GalleryItem>) -> Unit, onFailure: (Exception) -> Unit) {
        val firestore = FirebaseFirestore.getInstance()
        Log.d("FB_Gallery", "Fetching items for user UID: ${UserManager.getUser()!!.uid}")  // 디버그 로그 추가
        firestore.collection("recipe_follow")
            .whereEqualTo("userId",UserManager.getUser()!!.uid)
            .get()
            .addOnSuccessListener { querySnapshot ->
                Log.d("FB_Gallery", "Query successful. Documents found: ${querySnapshot.size()}")  // 쿼리 성공시 로그 추가
                if (querySnapshot.isEmpty) {
                    Log.d("FB_Gallery", "No items found for this user.")  // 결과가 없을 경우 로그 추가
                }
                // recipeId를 기반으로 추가적인 정보를 가져오기 위한 리스트 준비
                val tasks = mutableListOf<Task<DocumentSnapshot>>() // 각 recipeId 조회 작업 저장
                val items = querySnapshot.documents.mapNotNull { doc ->
                    val galleryItem = doc.toObject(GalleryItem::class.java)

                    galleryItem?.recipeId?.let { recipeId ->
                        // recipeId가 존재하는 경우, recipe/ 콜렉션에서 해당 recipeId 문서를 찾아 추가 정보 가져오기
                        // recipeId에 해당하는 recipe 문서 조회 작업 추가
                        val task = firestore.collection("recipe").document(recipeId).get()
                        tasks.add(task)

                        task.addOnSuccessListener { recipeDoc ->
                                if (recipeDoc.exists()) {
                                    val recipe = recipeDoc.toObject(RecipeItem::class.java)
                                    // recipe 정보를 galleryItem에 추가하거나 처리
                                    galleryItem.name = recipe!!.name
                                    galleryItem.image = recipe.imageResId
                                    Log.i("FB_Gallery", "galleryItem Added! $galleryItem")

                                } else {
                                    Log.d("FB_Gallery", "No recipe found for recipeId: $recipeId")
                                }
                            }
                            .addOnFailureListener { exception ->
                                Log.e("FB_Gallery", "Error fetching recipe details", exception)
                            }
                    }

                    galleryItem
                }
                // 모든 작업이 완료된 후 onSuccess 호출
                Tasks.whenAllComplete(tasks).addOnCompleteListener {
                    Log.d("FB_Gallery", "All recipe details fetched. Total items: ${items.size}")
                    onSuccess(items) // 최종 결과 반환
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FB_Gallery", "Error fetching data", exception)  // 실패시 오류 로그 추가
                onFailure(exception)
            }
    }
    fun setGridViewHeightBasedOnItems(gridView: GridView, adapter: GalleryGridAdapter, numColumns: Int) {
        val totalItems = adapter.count
        if (totalItems <= 0) return

        // 한 행에 들어가는 아이템 수 (numColumns) 기준으로 전체 행 수 계산
        val rows = (totalItems + numColumns - 1) / numColumns // 올림 처리
        var totalHeight = 0

        // 각 아이템의 높이를 측정
        for (i in 0 until rows) {
            val itemView = adapter.getView(i, null, gridView)
            itemView.measure(
                View.MeasureSpec.makeMeasureSpec(gridView.width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.UNSPECIFIED
            )
            totalHeight += itemView.measuredHeight
        }

        // 높이에 divider height 추가 (GridView의 rowSpacing을 반영)
        val verticalSpacing = gridView.verticalSpacing
        totalHeight += (rows - 1) * verticalSpacing

        // GridView의 레이아웃 파라미터 설정
        val params = gridView.layoutParams
        params.height = totalHeight
        gridView.layoutParams = params
        gridView.requestLayout()
    }
}