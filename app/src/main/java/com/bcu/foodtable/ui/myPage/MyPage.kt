package com.bcu.foodtable.ui.myPage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bcu.foodtable.R
import com.bcu.foodtable.databinding.FragmentMypageBinding
import com.bcu.foodtable.useful.FireStoreHelper.loadImageFromUrl
import com.bcu.foodtable.useful.User
import com.bcu.foodtable.useful.UserManager

class MyPage : Fragment() {

    private lateinit var userData: User

    private var _binding: FragmentMypageBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMypageBinding.inflate(inflater, container, false)
        val root: View = binding.root

        userData = UserManager.getUser()!!

        // 유저 프로필 관련
        val userProfileImage = binding.ProfileMyImage
        val userBackgroundImage = binding.ProfileBackground
        val userNameText = binding.ProfileMyName
        val userDescription = binding.ProfileIntroduceTextView
        val userSaltTextView = binding.ProfileMySalt
        val userSaltPurchaseButton = binding.ProfileSaltPurchaseBtn
        val userSubscribeRecycler = binding.ProfileSubscribeRecyclerView
        val userEditButton = binding.ProfileEditBtn
        // 유저 랭크 관련
        val userRankIcon = binding.ProfileRankIcon
        val userRankText = binding.ProfileRankText

        loadImageFromUrl(userData.image,userProfileImage)
        userBackgroundImage.setImageResource(R.drawable.food_image)
        userNameText.text = userData.Name
        userDescription.text = userData.description
        userSaltTextView.text= "${userData.point}"
        userRankText.text = "${userData.rankPoint}"
        
        // 랭크 점수에 따른 이미지 차이 예시
        if(userData.rankPoint<=50){
            userRankIcon.setImageResource(R.drawable.baseline_star_border_24)
        }else{
            userRankIcon.setImageResource(R.drawable.baseline_star_24)
        }

        userSaltPurchaseButton.setOnClickListener{
            // 결제 창으로 이어지는 동작을 만들 버튼
        }

        // 구독 관련 리사이클러뷰를 처리할 부분
        //
        //

        userEditButton.setOnClickListener{
            // 유저 정보 수정을 처리하는 부분
        }
        return root
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}