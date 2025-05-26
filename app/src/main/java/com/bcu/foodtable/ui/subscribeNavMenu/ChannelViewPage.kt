package com.bcu.foodtable.ui.subscribeNavMenu

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.GridView
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
import com.bcu.foodtable.useful.RecipeAdapter
import com.bcu.foodtable.useful.RecipeItem
import com.bcu.foodtable.useful.UserManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope


class ChannelViewPage : AppCompatActivity() {

    private val recipeList: MutableList<RecipeItem> = mutableListOf()
    private lateinit var adaptorViewList: GridView
    private lateinit var recipeAdapter: RecipeAdapter
    private lateinit var channelitem: Channel
    private lateinit var viewModel: k_ChannelViewModel

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
        val db = FirebaseFirestore.getInstance()
        val subscriberCountTextView = findViewById<TextView>(R.id.subscriberCount)



        // Adapter 초기화
        recipeAdapter = RecipeAdapter(this@ChannelViewPage, recipeList)
        adaptorViewList.adapter = recipeAdapter

        // 🔹 GridView 아이템 클릭 시 상세 페이지로 이동
        adaptorViewList.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val clickedRecipe = recipeAdapter.getItem(position) as? RecipeItem
            clickedRecipe?.let {
                val id = it.id ?: "" // 🔹 id가 null이면 빈 문자열로 처리
                Log.d("ChannelViewPage", "RecipeClicked: $id")

                val intent = Intent(this, RecipeViewActivity::class.java)
                intent.putExtra("recipe_id", id) // 🔹 Firestore 문서 ID 전달
                startActivity(intent)
            }
        }

        // 🔹 Write 버튼 클릭 리스너
        writeButton.setOnClickListener {
            val intent = Intent(this, WriteActivity::class.java)
            intent.putExtra("channel_name", channelitem.name)  // 🔹 Firestore 문서 ID 전달
            this.startActivity(intent)
        }

        // 🔹 현재 로그인된 사용자 ID 가져오기
        val user = UserManager.getUser()?.uid ?: ""
        // ViewModel 초기화
        viewModel = ViewModelProvider(this)[k_ChannelViewModel::class.java]
        // observe 등록
        viewModel.channel.observe(this) { channel ->
            channelitem = channel
            channelNameText.text = channel.name
            FireStoreHelper.loadImageFromUrl(channel.BackgroundResId, backgroundImg)
            FireStoreHelper.loadImageFromUrl(channel.imageResId, channelImg)

            writeButton.visibility = if (user == channel.owner) View.VISIBLE else View.GONE
            subscribeButton.visibility = if (user != channel.owner) View.VISIBLE else View.GONE
        }

        viewModel.subscriberCount.observe(this) { count ->
            subscriberCountTextView.text = "$count 명"
        }

        viewModel.isSubscribed.observe(this) { isSubscribed ->
            subscribeButton.text = if (isSubscribed) "구독중" else "구독하기"
        }

        viewModel.recipes.observe(this) { newRecipes ->
            recipeList.clear()
            recipeList.addAll(newRecipes)
            recipeAdapter.notifyDataSetChanged()
        }

        // 3. 클릭 리스너 설정
        subscribeButton.setOnClickListener {
            viewModel.toggleSubscription(channelitem.name, user)
        }

        writeButton.setOnClickListener {
            val intent = Intent(this, WriteActivity::class.java)
            intent.putExtra("channel_name", channelitem.name)
            startActivity(intent)
        }

        adaptorViewList.setOnItemClickListener { _, _, position, _ ->
            val item = recipeList[position]
            val intent = Intent(this, RecipeViewActivity::class.java)
            intent.putExtra("recipe_id", item.id)
            startActivity(intent)
        }
        // 🔹 채널 정보 불러오기
        lifecycleScope.launch {
            viewModel.loadChannel(channelName) // 채널 불러오기
            viewModel.loadRecipes(channelName) // 레시피 불러오기
            viewModel.checkSubscription(channelName, user) // 구독 여부
        }
    }

}
