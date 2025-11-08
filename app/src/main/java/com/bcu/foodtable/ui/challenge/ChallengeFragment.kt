package com.bcu.foodtable.ui.challenge

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bcu.foodtable.repository.ChallengeRepository
import com.bcu.foodtable.ui.ChallengeScreen
import com.bcu.foodtable.viewmodel.ChallengeViewModel

class ChallengeFragment : Fragment() {
 //// 🔥 ViewModel은 기본 생성자만 사용하므로, viewModels()만 사용
    private val viewModel: ChallengeViewModel by viewModels()
//    private val repository by lazy { ChallengeRepository() }
//
//    private val viewModel: ChallengeViewModel by viewModels {
//        object : ViewModelProvider.Factory {
//            override fun <T : ViewModel> create(modelClass: Class<T>): T {
//                if (modelClass.isAssignableFrom(ChallengeViewModel::class.java)) {
//                    @Suppress("UNCHECKED_CAST")
//                    return ChallengeViewModel(repository) as T
//                }
//                throw IllegalArgumentException("Unknown ViewModel class")
//            }
//        }
//    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    Surface(color = MaterialTheme.colorScheme.background) {
                        ChallengeScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
