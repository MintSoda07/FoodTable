//package com.bcu.foodtable.ui.subscribeNavMenu
//
//import android.content.Intent
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.fragment.app.Fragment
//import androidx.lifecycle.ViewModelProvider
//import androidx.recyclerview.widget.LinearLayoutManager
//// import com.bcu.foodtable.JetpackCompose.Subscribe.Channel.ChannelViewPage
//import com.bcu.foodtable.JetpackCompose.Subscribe.Channel.ChannelViewPageScreen
//import com.bcu.foodtable.databinding.FragmentSubscribeBinding
//import com.bcu.foodtable.useful.SubscribedChannelGridView
//
//class SubscribeFragment : Fragment() {
//
//    private var _binding: FragmentSubscribeBinding? = null
//    private val binding get() = _binding!!
//    private lateinit var viewModel: SubscribeViewModel
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
//    ): View {
//        _binding = FragmentSubscribeBinding.inflate(inflater, container, false)
//        val root = binding.root
//
//        viewModel = ViewModelProvider(this)[SubscribeViewModel::class.java]
//
//
//        // 상단: 구독한 채널
//        viewModel.subscribedChannels.observe(viewLifecycleOwner) { items ->
//            binding.SubscribeItemsGrid.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
//            binding.SubscribeItemsGrid.adapter = SubscribedChannelGridView(
//                context = requireContext(),
//                itemList = items,
//                onClick = { item ->
//                    val intent = Intent(requireContext(), ChannelViewPage::class.java)
//                    intent.putExtra("channel_name", item.name)
//                    startActivity(intent)
//                }
//            )
//        }
//
//        // 중단: 내가 만든 채널
//        viewModel.myChannels.observe(viewLifecycleOwner) { items ->
//            binding.SubscribeMyChannelGrid.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
//            binding.SubscribeMyChannelGrid.adapter = SubscribedChannelGridView(
//                context = requireContext(),
//                itemList = items,
//                onClick = { item ->
//                    val intent = Intent(requireContext(), ChannelViewPage::class.java)
//                    intent.putExtra("channel_name", item.name)
//                    startActivity(intent)
//                }
//            )
//        }
//
//        // 하단: 추천 채널
//        viewModel.recommendedChannels.observe(viewLifecycleOwner) { items ->
//            binding.SubscribeAllChannelGrid.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
//            binding.SubscribeAllChannelGrid.adapter = SubscribedChannelGridView(
//                context = requireContext(),
//                itemList = items,
//                onClick = { item ->
//                    val intent = Intent(requireContext(), ChannelViewPage::class.java)
//                    intent.putExtra("channel_name", item.name)
//                    startActivity(intent)
//                }
//            )
//        }
//
//        //  데이터 불러오기
//        viewModel.fetchSubscribedChannels()
//        viewModel.fetchMyChannels()
//        viewModel.fetchRecommendedChannels()
//
//        return root
//    }
//    override fun onResume() {
//        super.onResume()
//        // 자동 새로고침
//        viewModel.fetchSubscribedChannels()
//        viewModel.fetchMyChannels()
//        viewModel.fetchRecommendedChannels()
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        _binding = null
//    }
//}
