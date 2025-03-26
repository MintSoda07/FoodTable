package com.bcu.foodtable.ui.subscribeNavMenu

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bcu.foodtable.R
import com.bcu.foodtable.RecipeViewActivity
import com.bcu.foodtable.useful.Channel
import com.bcu.foodtable.useful.FireStoreHelper
import com.bcu.foodtable.useful.FirebaseHelper
import com.bcu.foodtable.useful.FirebaseHelper.updateFieldById
import com.bcu.foodtable.useful.RecipeItem
import com.bcu.foodtable.useful.UserManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ChannelViewPage : AppCompatActivity() {
    lateinit var channelitem: Channel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_channel_view_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val channelName = intent.getStringExtra("channel_name") ?: ""
        val backgroundImg = findViewById<ImageView>(R.id.channelBackground)
        val channelImg = findViewById<ImageView>(R.id.channelImage)
        val channelNameText = findViewById<TextView>(R.id.channelName)
        val writeButton: Button = findViewById(R.id.btn_write)
        val subscribeButton : Button = findViewById(R.id.subbtn)

        writeButton.setOnClickListener {
            val intent = Intent(this, WriteActivity::class.java)
            intent.putExtra("channel_name", channelitem.name)  // Firestore 문서 ID 전달
            this.startActivity(intent)  // 새로운 액티비티로 전환
        }
        // 유저 UID 불러오기 (현재 유저)
        val user = UserManager.getUser()!!.uid



        CoroutineScope(Dispatchers.Main).launch {
            channelitem = getChannelByName(channelName)!!
            FireStoreHelper.loadImageFromUrl(channelitem.BackgroundResId,backgroundImg)
            FireStoreHelper.loadImageFromUrl(channelitem.imageResId,channelImg)
            channelNameText.text = channelitem.name
            //유저 UID와 채널주인 UID가 같은지 확인 (String)
            if (user.equals(channelitem.owner)) {
                // 채널 주인일 경우
                writeButton.visibility = View.VISIBLE
                subscribeButton.visibility = View.GONE
            } else {
                // 일반 사용자일 경우
                writeButton.visibility = View.GONE
                subscribeButton.visibility = View.VISIBLE
            }
        }
    }
    suspend fun getChannelByName(channelName: String): Channel? {
        return withContext(Dispatchers.IO) {
            try {
                val db = FirebaseFirestore.getInstance()

                // "channel" 컬렉션에서 name 필드가 channelName과 일치하는 문서 찾기
                val query = db.collection("channel")
                    .whereEqualTo("name", channelName)
                    .get()
                    .await()

                // 쿼리 결과에서 문서를 가져와 Channel 객체로 변환
                if (query.isEmpty) {
                    null
                } else {
                    val channelItem = query.documents[0].toObject(Channel::class.java)
                    channelItem
                }
            } catch (e: Exception) {
                // 오류 발생 시 null 반환
                e.printStackTrace()
                null
            }
        }
    }
}