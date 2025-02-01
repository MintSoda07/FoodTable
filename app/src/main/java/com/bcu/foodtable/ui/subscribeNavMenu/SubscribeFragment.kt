package com.bcu.foodtable.ui.subscribeNavMenu

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bcu.foodtable.databinding.FragmentSubscribeBinding
import com.bcu.foodtable.useful.Channel
import com.bcu.foodtable.useful.GalleryGridAdapter
import com.bcu.foodtable.useful.SubscribedChannelGridView
import com.bcu.foodtable.useful.UserManager
import com.google.firebase.firestore.FirebaseFirestore

class SubscribeFragment : Fragment() {

    private var _binding: FragmentSubscribeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var topMySubscribes: RecyclerView
    private lateinit var subscribeRoundAdaptor: SubscribedChannelGridView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSubscribeBinding.inflate(inflater, container, false)
        val root: View = binding.root



        fetchChannels(
            onSuccess = { items->
                topMySubscribes= binding.SubscribeItemsGrid
                Log.d(
                    "FB_Subscribe",
                    "Items Loaded with Size : ${items.size}"
                )
                subscribeRoundAdaptor = SubscribedChannelGridView(
                    context = requireContext(),
                    itemList = items
                )
                topMySubscribes.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                topMySubscribes.adapter = subscribeRoundAdaptor
                Log.d(
                    "FB_Subscribe",
                    "Fetching items Successfully after Data Loading."
                )
            },
            onFailure = {
                Log.d(
                    "FB_Subscribe",
                    "Fetching items failed after Data Loading."
                )
            }
        )


        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun fetchChannels(onSuccess: (List<Channel>) -> Unit, onFailure: (Exception) -> Unit) {
        val firestore = FirebaseFirestore.getInstance()
        Log.d(
            "FB_Subscribe",
            "Fetching items for user UID: ${UserManager.getUser()!!.uid}"
        )  // 디버그 로그 추가
        firestore.collection("channel")
            .orderBy("date") // "subscribers" 필드로 정렬
            .limit(20) //일단 20개까지만 불러온다.
            .get()
            .addOnSuccessListener { querySnapshot ->
                Log.d(
                    "FB_Subscribe",
                    "Query successful. Documents found: ${querySnapshot.size()}"
                )  // 쿼리 성공시 로그 추가
                if (querySnapshot.isEmpty) {
                    Log.d("FB_Gallery", "No items found.")  // 결과가 없을 경우 로그 추가
                }
                // recipeId를 기반으로 추가적인 정보를 가져오기 위한 리스트 준비
                val items = querySnapshot.documents.mapNotNull { doc ->
                    val topRecyclerViewItem = doc.toObject(Channel::class.java)
                    topRecyclerViewItem
                }
                // 모든 작업이 완료된 후 onSuccess 호출
                Log.d("FB_Subscribe", "All recipe details fetched. Total items: ${items.size}")
                onSuccess(items) // 최종 결과 반환
            }
            .addOnFailureListener { exception ->
                Log.e("FB_Subscribe", "Error fetching data", exception)  // 실패시 오류 로그 추가
                onFailure(exception)
            }
    }

}