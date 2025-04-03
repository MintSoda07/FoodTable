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

        // 🔹 채널 정보 불러오기
        CoroutineScope(Dispatchers.Main).launch {
            channelitem = getChannelByName(channelName) ?: return@launch

            // 🔹 Firestore에서 이미지 불러오기
            FireStoreHelper.loadImageFromUrl(channelitem.BackgroundResId, backgroundImg)
            FireStoreHelper.loadImageFromUrl(channelitem.imageResId, channelImg)

            // 🔹 채널 이름 설정
            channelNameText.text = channelitem.name

            // 🔹 작성자와 비교하여 버튼 설정
            if (user == channelitem.owner) {
                writeButton.visibility = View.VISIBLE
                subscribeButton.visibility = View.GONE
            } else {
                writeButton.visibility = View.GONE
                subscribeButton.visibility = View.VISIBLE
            }

            // 🔹 레시피 목록 불러오기
            loadRecipes()
        }
    }

    // 🔹 Firestore에서 채널 정보 가져오기
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

    // 🔹 레시피 목록을 Firestore에서 불러오기
    private fun loadRecipes() {
        CoroutineScope(Dispatchers.IO).launch {
            val db = FirebaseFirestore.getInstance()
            try {
                val querySnapshot = db.collection("recipe")
                    .whereEqualTo("contained_channel", channelitem.name)
                    .get()
                    .await()

                val recipes = querySnapshot.documents.mapNotNull { doc ->
                    val recipe = doc.toObject(RecipeItem::class.java)
                    recipe?.id = doc.id // 🔹 Firestore 문서 ID를 RecipeItem 객체에 설정
                    recipe
                }

                withContext(Dispatchers.Main) {
                    // 🔹 레시피 목록 업데이트
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
