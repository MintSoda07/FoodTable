package com.bcu.foodtable.ui.subscribeNavMenu

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bcu.foodtable.RecipeViewActivity
import com.bcu.foodtable.databinding.FragmentSubscribeBinding
import com.bcu.foodtable.useful.Channel
import com.bcu.foodtable.useful.GalleryGridAdapter
import com.bcu.foodtable.useful.GalleryItem
import com.bcu.foodtable.useful.RecipeItem
import com.bcu.foodtable.useful.SubscribeItem
import com.bcu.foodtable.useful.SubscribedChannelGridView
import com.bcu.foodtable.useful.UserManager
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class SubscribeFragment : Fragment() {

    private var _binding: FragmentSubscribeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var topMySubscribes: RecyclerView
    private lateinit var middleMyChannel: RecyclerView
    private lateinit var bottomAllChannel: RecyclerView


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSubscribeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        fetchSubscribedChannels(
            onSuccess = { items->
                topMySubscribes= binding.SubscribeItemsGrid
                Log.d(
                    "FB_Subscribe",
                    "Items Loaded with Size : ${items.size}"
                )
                val subscribeRoundAdaptor = SubscribedChannelGridView(
                    context = requireContext(),
                    itemList = items,
                    onClick = { item->
                        val channelItem = item.name
                        val intent = Intent(context, ChannelViewPage::class.java)
                        intent.putExtra("channel_name", channelItem)  // Firestore 문서 ID 전달
                        context?.startActivity(intent)  // 새로운 액티비티로 전환
                    }
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
        fetchMyChannels(
            onSuccess = { items->
                middleMyChannel= binding.SubscribeMyChannelGrid
                Log.d(
                    "FB_Subscribe",
                    "Items Loaded with Size : ${items.size}"
                )
                val subscribeMyChannelRoundAdaptor = SubscribedChannelGridView(
                    context = requireContext(),
                    itemList = items,
                    onClick = { item->
                        val channelItem = item.name
                        val intent = Intent(context, ChannelViewPage::class.java)
                        intent.putExtra("channel_name", channelItem)  // Firestore 문서 ID 전달
                        context?.startActivity(intent)  // 새로운 액티비티로 전환
                    }
                )
                middleMyChannel.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                middleMyChannel.adapter = subscribeMyChannelRoundAdaptor
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
        fetchRecommendedChannels(
            onSuccess = { items->
                bottomAllChannel= binding.SubscribeAllChannelGrid
                Log.d(
                    "FB_Subscribe",
                    "Items Loaded with items : ${items}"
                )
                val subscribeMyChannelRoundAdaptor = SubscribedChannelGridView(
                    context = requireContext(),
                    itemList = items,
                    onClick = { item->
                        val channelItem = item.name
                        val intent = Intent(context, ChannelViewPage::class.java)
                        intent.putExtra("channel_name", channelItem)  // Firestore 문서 ID 전달
                        context?.startActivity(intent)  // 새로운 액티비티로 전환
                    }
                )
                bottomAllChannel.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                bottomAllChannel.adapter = subscribeMyChannelRoundAdaptor
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

    fun fetchMyChannels(onSuccess: (List<Channel>) -> Unit, onFailure: (Exception) -> Unit) {
        val firestore = FirebaseFirestore.getInstance()
        Log.d(
            "FB_Subscribe",
            "Fetching items for user UID: ${UserManager.getUser()!!.uid}"
        )  // 디버그 로그 추가
        firestore.collection("channel")
            .whereEqualTo("owner", UserManager.getUser()!!.uid)
            .orderBy("date")
            .get()
            .addOnSuccessListener { querySnapshot ->
                Log.d(
                    "FB_Subscribe",
                    "Query successful. Documents found: ${querySnapshot.size()}"
                )  // 쿼리 성공시 로그 추가
                if (querySnapshot.isEmpty) {
                    Log.d("FB_Subscribe", "No items found.")  // 결과가 없을 경우 로그 추가
                }
                val items = querySnapshot.documents.mapNotNull { doc ->
                    val topRecyclerViewItem = doc.toObject(Channel::class.java)
                    topRecyclerViewItem
                }
                Log.d("FB_Subscribe", "All recipe details fetched. Total items: ${items.size}")
                onSuccess(items) // 최종 결과 반환
            }
            .addOnFailureListener { exception ->
                Log.e("FB_Subscribe", "Error fetching data", exception)  // 실패시 오류 로그 추가
                onFailure(exception)
            }
    }


    fun fetchSubscribedChannels(onSuccess: (List<Channel>) -> Unit, onFailure: (Exception) -> Unit) {
        val firestore = FirebaseFirestore.getInstance()
        Log.d(
            "FB_Subscribe",
            "Fetching items for user UID: ${UserManager.getUser()!!.uid}"
        )  // 디버그 로그 추가
        firestore.collection("channel_subscribe")
            .whereEqualTo("userId", UserManager.getUser()!!.uid)
            .orderBy("date")
            .limit(40) //일단 40개까지만 불러온다.
            .get()
            .addOnSuccessListener { querySnapshot ->
                Log.d(
                    "FB_Subscribe",
                    "Query successful. Documents found: ${querySnapshot.size()}"
                )  // 쿼리 성공시 로그 추가
                if (querySnapshot.isEmpty) {
                    Log.d("FB_Subscribe", "No items found.")
                }
                val tasks = mutableListOf<Task<DocumentSnapshot>>() // 개별 조회 작업 저장
                val channelList = mutableListOf<Channel>() // 최종 채널 리스트

                for (doc in querySnapshot.documents) {
                    val channelId = doc.getString("channel") ?: continue // `channelId` 가져오기

                    // channel 컬렉션에서 해당 channelId 문서 가져오기
                    val task = firestore.collection("channel").document(channelId).get()
                    tasks.add(task)

                    task.addOnSuccessListener { channelDoc ->
                        if (channelDoc.exists()) {
                            val channelItem = channelDoc.toObject(Channel::class.java)
                            channelItem?.let {
                                channelList.add(it)
                                Log.i("FB_Subscribe", "Subscribed Channel Added: $it")
                            }
                        } else {
                            Log.d("FB_Subscribe", "No Channel found for ID: $channelId")
                        }
                    }.addOnFailureListener { exception ->
                        Log.e("FB_Subscribe", "Error fetching channel data", exception)
                    }
                }

                // 3️ 모든 Firestore 요청이 끝난 후 `onSuccess` 호출
                Tasks.whenAllComplete(tasks).addOnCompleteListener {
                    Log.d("FB_Subscribe", "All channels fetched. Total items: ${channelList.size}")
                    onSuccess(channelList) // 최종 리스트 반환
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FB_Subscribe", "Error fetching data", exception)  // 실패시 오류 로그 추가
                onFailure(exception)
            }
    }

    fun fetchRecommendedChannels(onSuccess: (List<Channel>) -> Unit, onFailure: (Exception) -> Unit) {
        val firestore = FirebaseFirestore.getInstance()
        Log.d(
            "FB_Subscribe",
            "Fetching Recommended Channels for user UID: ${UserManager.getUser()!!.uid}"
        )  // 디버그 로그 추가
        firestore.collection("channel")
            .orderBy("subscribers")
            .get()
            .addOnSuccessListener { querySnapshot ->
                Log.d(
                    "FB_Subscribe",
                    "(recommendedChannel) Query successful. Documents found: ${querySnapshot.size()}"
                )  // 쿼리 성공시 로그 추가
                if (querySnapshot.isEmpty) {
                    Log.d("FB_Subscribe", "No items found.")  // 결과가 없을 경우 로그 추가
                }
                val items = querySnapshot.documents.mapNotNull { doc ->
                    val topRecyclerViewItem = doc.toObject(Channel::class.java)
                    topRecyclerViewItem
                }
                Log.d("FB_Subscribe", "All recipe details fetched. Total items: ${items.size}")
                onSuccess(items) // 최종 결과 반환
            }
            .addOnFailureListener { exception ->
                Log.e("FB_Subscribe", "Error fetching data", exception)  // 실패시 오류 로그 추가
                onFailure(exception)
            }
    }
}