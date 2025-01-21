package com.bcu.foodtable.ui.aiServiceNavMenu

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bcu.foodtable.AI.OpenAIClient
import com.bcu.foodtable.databinding.FragmentAiBinding

class AIFragment : Fragment() {

    private var _binding: FragmentAiBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAiBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val aIServiceManager = OpenAIClient()
        aIServiceManager.setAIWithAPI(
            onSuccess = {
                aIServiceManager.sendMessage(
                    prompt = "안녕하세요, 쿡봇! 이것은 테스트 수신입니다. 수신하였다면 아무 농담이나 입력해 주세요!", // 사용자 입력
                    onSuccess = { response ->
                        println("ChatGPT 응답: $response")
                    },
                    onError = { error ->
                        println("오류: $error")

                    }
                )
            },
            onError = {
                Log.e("AI_SERVICE","An Error Occured During Setting an AI.")
            }
        )

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}