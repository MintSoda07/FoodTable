package com.bcu.foodtable.useful

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bcu.foodtable.R
import java.text.SimpleDateFormat
import java.util.Locale

class ChattingAdaptor:RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    val chatList= mutableListOf<AIChatting>()
    val chatLeft=0
    val chatRight=1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == chatLeft) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.chat_bubble_left, parent, false)
            LeftChatViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.chat_bubble_right, parent, false)
            RightChatViewHolder(view)
        }
    }

    override fun getItemCount(): Int {
        return chatList.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val chat = chatList[position]
        if (holder is LeftChatViewHolder) {
            holder.bind(chat)
        } else if (holder is RightChatViewHolder) {
            holder.bind(chat)
        }
    }

    override fun getItemViewType(position: Int): Int {
        // 좌우 채팅을 구분
        // 예시: content가 "Partner:"로 시작하면 왼쪽 채팅, 아니면 오른쪽 채팅
        return if (chatList[position].content.startsWith("Partner:")) chatLeft else chatRight
    }

    // 왼쪽 채팅 ViewHolder
    class LeftChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val chatTextView: TextView = itemView.findViewById(R.id.InnerTextChat)
        private val chatName: TextView  = itemView.findViewById(R.id.NameChat)
        private val imageProfile : ImageView = itemView.findViewById(R.id.Profilechat)
        private val timeStampView : TextView = itemView.findViewById(R.id.TimeChat)
        fun bind(chat: AIChatting) {
            chatTextView.text =chat.content.replace("Partner:", "").trim()
            chatName.text ="AI Service"
            imageProfile.setImageResource(R.drawable.dish_icon)
            imageProfile.backgroundTintList = ContextCompat.getColorStateList(itemView.context, R.color.ic_launcher_babsang_background)

            // "a"는 AM/PM, "hh"는 12시간 형식 이런 느낌으로 형태를 바꿔 준다
            val dateFormat = SimpleDateFormat("a hh:mm", Locale.getDefault())
            val formattedTime = dateFormat.format(chat.chatDate!!.toDate())
            timeStampView.text = formattedTime
        }
    }

    // 오른쪽 채팅 ViewHolder
    class RightChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val chatTextView: TextView = itemView.findViewById(R.id.InnerTextChat)
        private val chatName: TextView  = itemView.findViewById(R.id.NameChat)
        private val imageProfile : ImageView = itemView.findViewById(R.id.Profilechat)
        private val timeStampView : TextView = itemView.findViewById(R.id.TimeChat)

        fun bind(chat: AIChatting) {
            chatTextView.text = chat.content
            chatName.text = UserManager.getUser()!!.name
            FireStoreHelper.loadImageFromUrl(UserManager.getUser()!!.image,imageProfile)


            // "a"는 AM/PM, "hh"는 12시간 형식 이런 느낌으로 형태를 바꿔 준다
            val dateFormat = SimpleDateFormat("a hh:mm", Locale.getDefault())
            val formattedTime = dateFormat.format(chat.chatDate!!.toDate())
            timeStampView.text = formattedTime
        }
    }

    // 데이터 추가 함수
    fun addChat(chat: AIChatting) {
        chatList.add(chat)
        notifyItemInserted(chatList.size - 1)
    }
}