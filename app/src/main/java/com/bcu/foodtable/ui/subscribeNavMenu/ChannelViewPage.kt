package com.bcu.foodtable.ui.subscribeNavMenu

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.GridView
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.helper.widget.Grid
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bcu.foodtable.R
import com.bcu.foodtable.RecipeViewActivity
import com.bcu.foodtable.useful.Channel
import com.bcu.foodtable.useful.FireStoreHelper
import com.bcu.foodtable.useful.FirebaseHelper
import com.bcu.foodtable.useful.RecipeAdapter
import com.bcu.foodtable.useful.RecipeItem
import com.bcu.foodtable.useful.UserManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ChannelViewPage : AppCompatActivity() {

    private val recipeList: MutableList<RecipeItem> = mutableListOf()
    private lateinit var adaptorViewList: GridView
    private lateinit var recipeAdapter: RecipeAdapter
    private lateinit var channelitem: Channel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_channel_view_page)

        // 윈도우 인셋 적용
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 채널명과 관련된 UI 요소들 초기화
        val channelName = intent.getStringExtra("channel_name") ?: ""
        val backgroundImg = findViewById<ImageView>(R.id.channelBackground)
        val channelImg = findViewById<ImageView>(R.id.channelImage)
        val channelNameText = findViewById<TextView>(R.id.channelName)
        val writeButton: Button = findViewById(R.id.btn_write)
        val subscribeButton: Button = findViewById(R.id.subbtn)
        adaptorViewList = findViewById<GridView>(R.id.channelItem)

        // Adapter 초기화 (Context를 포함해서 생성)
        recipeAdapter = RecipeAdapter(this@ChannelViewPage, recipeList)
        adaptorViewList.adapter = recipeAdapter

        // Write 버튼 클릭 리스너
        writeButton.setOnClickListener {
            val intent = Intent(this, WriteActivity::class.java)
            intent.putExtra("channel_name", channelitem.name)  // Firestore 문서 ID 전달
            this.startActivity(intent)  // 새로운 액티비티로 전환
        }

        // 현재 로그인된 사용자 ID
        val user = UserManager.getUser()?.uid ?: ""

        // 채널 정보 로드
        CoroutineScope(Dispatchers.Main).launch {
            channelitem = getChannelByName(channelName) ?: return@launch

            // Firestore에서 이미지 로드
            FireStoreHelper.loadImageFromUrl(channelitem.BackgroundResId, backgroundImg)
            FireStoreHelper.loadImageFromUrl(channelitem.imageResId, channelImg)

            // 채널 이름 텍스트 설정
            channelNameText.text = channelitem.name

            // 작성자와 비교하여 버튼 설정
            if (user == channelitem.owner) {
                writeButton.visibility = View.VISIBLE
                subscribeButton.visibility = View.GONE
            } else {
                writeButton.visibility = View.GONE
                subscribeButton.visibility = View.VISIBLE
            }

            // 레시피 목록 불러오기
            loadRecipes(channelitem.owner)
        }
    }

    // Firestore에서 채널 정보 가져오기
    private suspend fun getChannelByName(channelName: String): Channel? {
        return withContext(Dispatchers.IO) {
            try {
                val db = FirebaseFirestore.getInstance()
                val query = db.collection("channel")
                    .whereEqualTo("name", channelName)
                    .get()
                    .await()

                if (query.isEmpty) null else query.documents[0].toObject(Channel::class.java)
            } catch (e: Exception) {
                Log.e("Firestore", "채널 가져오기 실패: ${e.message}")
                null
            }
        }
    }

    // 레시피 목록을 Firestore에서 불러오기
    private fun loadRecipes(ownerUid: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = FirebaseFirestore.getInstance()
            try {
                val querySnapshot = db.collection("recipes")
                    .whereEqualTo("author", ownerUid)
                    .get()
                    .await()

                val recipes =
                    querySnapshot.documents.mapNotNull { it.toObject(RecipeItem::class.java) }

                withContext(Dispatchers.Main) {
                    // 레시피 목록 업데이트
                    recipeList.clear()
                    recipeList.addAll(recipes)
                    recipeAdapter.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                Log.e("Firestore", "레시피 불러오기 실패: ${e.message}")
            }
        }
    }
}