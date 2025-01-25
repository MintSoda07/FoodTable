package com.bcu.foodtable.ui.myPage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bcu.foodtable.databinding.FragmentMypageBinding
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

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}