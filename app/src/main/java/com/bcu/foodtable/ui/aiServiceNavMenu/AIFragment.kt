package com.bcu.foodtable.ui.aiServiceNavMenu

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.fragment.app.Fragment
import com.bcu.foodtable.AI.AiRecommendationActivity
import com.bcu.foodtable.databinding.FragmentAiBinding
import com.bcu.foodtable.useful.ActivityTransition
import com.bcu.foodtable.useful.ViewAnimator

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

        val cardItem_1 = binding.CardRecommendation
        val cardItem_2 = binding.CardChatting
        val cardItem_3 = binding.CardAIAssistant

        // 좌우에서 나타나는 애니메이션
        ViewAnimator.moveXPos(cardItem_1, -900f, -120f, 380, DecelerateInterpolator(3.0f), {
            ViewAnimator.moveXPos(
                cardItem_2,
                900f,
                120f,
                380,
                DecelerateInterpolator(3.0f),
                {
                    ViewAnimator.moveXPos(cardItem_3, -900f, -120f, 380, DecelerateInterpolator(3.0f))
                        .start()
                }).start()
        }).start()

        // 클릭 시 효과
        cardItem_1.setOnClickListener{
            // AI 추천
            ActivityTransition.startStaticInFragment(context,AiRecommendationActivity::class.java)
        }
        cardItem_2.setOnClickListener{
            // AI 채팅
        }
        cardItem_3.setOnClickListener{
            // AI 도우미
        }




        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}