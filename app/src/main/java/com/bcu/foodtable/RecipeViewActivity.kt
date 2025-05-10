package com.bcu.foodtable

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.GridView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bcu.foodtable.AI.OpenAIClient
import com.bcu.foodtable.RecipeViewActivity.Comment
import com.bcu.foodtable.ui.subscribeNavMenu.WriteActivity
import com.bcu.foodtable.useful.*
import com.bcu.foodtable.useful.FirebaseHelper.updateFieldById
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.firebase.firestore.Query

class RecipeViewActivity : AppCompatActivity() {
    private lateinit var recipeId: String
    private lateinit var adaptorViewList: RecyclerView
    private lateinit var RecipeAdaptor: RecipeDetailRecyclerAdaptor
    private lateinit var items: List<String>
    private lateinit var commentRecyclerView: RecyclerView
    private lateinit var commentAdapter: CommentAdapter
    private lateinit var commentEditText: EditText
    private lateinit var commentSendButton: Button
    private lateinit var deleteBtn: Button
    private lateinit var deleteRecipeButton: Button
    private lateinit var editBtn: Button
    private val db = FirebaseFirestore.getInstance()

    private var isClickedUpdated = false
    private lateinit var notificationPermissionManager: NotificationPermissionManager

    //    private val regex = Regex("(.*)\\s*\\((.*),(\\d{2}:\\d{2}:\\d{2})\\)") // 타이머가 포함된 형식
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_recipe_view)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }



        deleteRecipeButton = findViewById(R.id.deleteRecipeButton)
        editBtn = findViewById(R.id.editRecipeButton)
        // 레시피 수정 기능 (editBtn 클릭 시 수정 화면으로 이동)
        editBtn.setOnClickListener {
            val intent = Intent(this, WriteActivity::class.java)
            intent.putExtra("recipe_id", recipeId) // 수정할 레시피 ID 전달
            startActivity(intent)
        }
        deleteRecipeButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("레시피 삭제")
                .setMessage("정말 이 레시피를 삭제하시겠습니까?")
                .setPositiveButton("삭제") { _, _ ->
                    deleteRecipe(recipeId) // 네가 이미 만든 삭제 함수
                }
                .setNegativeButton("취소", null)
                .show()
        }
        //AI 호출
        if (ApiKeyManager.getGptApi() == null) {
            val aIServiceAgent = OpenAIClient()
            aIServiceAgent.setAIWithAPI(
                onSuccess = { info ->
                    Log.i("OpenAI", "API Name: ${info.KEY_NAME}")
                    Log.i("OpenAI", "API Key Successfully loaded.")
                    ApiKeyManager.setGptApiKey(info.KEY_NAME!!, info.KEY_VALUE!!)

                },
                onError = {
                    Log.e("OpenAI", "Failed to Load OpenAI API Key.")
                }
            )
        }

      deleteRecipeButton = findViewById(R.id.deleteRecipeButton)
      editBtn = findViewById(R.id.editRecipeButton)


        // 댓글 관련 초기화
        commentRecyclerView = findViewById(R.id.commentRecyclerView)
        commentEditText = findViewById(R.id.commentEditText)
        commentSendButton = findViewById(R.id.commentSendButton)

        commentRecyclerView.layoutManager = LinearLayoutManager(this)
        commentAdapter = CommentAdapter(mutableListOf())
        commentRecyclerView.adapter = commentAdapter

        adaptorViewList = findViewById(R.id.AddPageStageListRecyclerView)
        adaptorViewList.layoutManager = LinearLayoutManager(this)

        deleteBtn = findViewById(R.id.idontwannadothis)
        // Intent로 전달된 데이터 받기
        recipeId = intent.getStringExtra("recipe_id") ?: ""

        // 댓글 불러오기
        loadComments()

        // 댓글 전송 버튼 클릭 이벤트
        commentSendButton.setOnClickListener {
            val commentText = commentEditText.text.toString().trim()
            if (commentText.isNotEmpty()) {
                postComment(commentText)
                commentEditText.text.clear()
            } else {
                Toast.makeText(this, "댓글을 입력하세요.", Toast.LENGTH_SHORT).show()
            }
        }


        val permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    // 알림 승인
                    Toast.makeText(this, "알림이 승인되었습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    // 알림 거부
                }
            }
        notificationPermissionManager = NotificationPermissionManager(this, permissionLauncher)

        // 알림 권한 요청
        notificationPermissionManager.requestPermissionIfNeeded()


        // Intent로 전달된 데이터 받기
        recipeId = intent.getStringExtra("recipe_id") ?: ""
        CoroutineScope(Dispatchers.Main).launch {
            val recipe = FirebaseHelper.getDocumentById("recipe", recipeId, RecipeItem::class.java)
            recipe?.let {
                it.id = recipeId  // Firestore 문서의 ID를 recipe.id에 할당
                val calorieTextView = findViewById<TextView>(R.id.estimatedCaloriesView)

                if (it.estimatedCalories.isNullOrBlank()) {
                    // Firestore에 칼로리 없으면 AI로 요청
                    estimateCaloriesWithAI(it) { result ->
                        calorieTextView.text = "예상 칼로리: $result"
                        // 결과 저장
                        db.collection("recipe").document(recipeId)
                            .update("estimatedCalories", result)
                            .addOnSuccessListener {
                                Log.d("CALORIE_AI", "칼로리 Firestore 저장 성공")
                            }
                            .addOnFailureListener { e ->
                                Log.e("CALORIE_AI", "칼로리 저장 실패", e)
                            }
                    }
                } else {
                    // Firestore에 이미 있으면 바로 표시
                    calorieTextView.text = "예상 칼로리: ${it.estimatedCalories}"
                }

                // 해당 아이템의 클릭 수 +1
                if (!isClickedUpdated) {
                    updateFieldById(
                        collectionPath = "recipe", // 컬렉션 이름
                        documentId = recipeId, // 문서 ID
                        fieldName = "clicked", // 수정할 필드 이름
                        newValue = it.clicked + 1 // 새 값
                    )
                }
                // UI 업데이트
                val placeholder_name = findViewById<TextView>(R.id.itemName_recipe)
                val placeholder_description = findViewById<TextView>(R.id.BasicDescription)
                val placeholder_image = findViewById<ImageView>(R.id.itemImageView)
                val inputString = it.order

                val placeholder_ingredients =
                    findViewById<RecyclerView>(R.id.itemIngredientsRecycler)
                placeholder_ingredients.layoutManager = LinearLayoutManager(this@RecipeViewActivity)
                val adaptor_ingre = IngredientAdapter(
                    it.ingredients.toMutableList(),
                    this@RecipeViewActivity,
                    onButtonClick = { click ->
                        Log.d("Log_LINK", "CLICKED")
                    },
                )
                placeholder_ingredients.adapter = adaptor_ingre
                val placeholder_categories = findViewById<RecyclerView>(R.id.categories)
                val placeholder_tags = findViewById<RecyclerView>(R.id.ItemTags)

                val placeholder_note = findViewById<TextView>(R.id.itemUserNote)

                val layoutManager = FlexboxLayoutManager(this@RecipeViewActivity).apply {
                    flexDirection = FlexDirection.ROW   // 행(row) 방향으로 아이템 배치
                    justifyContent = JustifyContent.FLEX_START // 아이템을 왼쪽 정렬
                    flexWrap = FlexWrap.WRAP           // 줄바꿈 허용 (자동으로 아이템 크기 맞추기)
                }
                val layoutManager2 = FlexboxLayoutManager(this@RecipeViewActivity).apply {
                    flexDirection = FlexDirection.ROW   // 행(row) 방향으로 아이템 배치
                    justifyContent = JustifyContent.FLEX_START // 아이템을 왼쪽 정렬
                    flexWrap = FlexWrap.WRAP           // 줄바꿈 허용 (자동으로 아이템 크기 맞추기)
                }

                // 기본적으로 안 보이게 설정
                deleteRecipeButton.visibility = View.GONE
                editBtn.visibility = View.GONE

                checkIfUserIsOwner()
                Log.d("Recipe_Edit_BtnCheck","유저이름은 :"+UserManager.getUser()!!.uid)


                // ○를 기준으로 문자열을 나눔 (
                items = inputString.split("○").filter { it.isNotBlank() }
                Log.d("Recipe", "현재 분리된 레시피 단계 : ${items}")
                // 리스트 어댑터
                RecipeAdaptor = RecipeDetailRecyclerAdaptor(
                    mutableListOf(),
                    this@RecipeViewActivity
                ) { position ->
                    onDoneButtonClick(position)
                }
                RecipeAdaptor.updateItems(items)
                adaptorViewList.adapter = RecipeAdaptor
                FireStoreHelper.loadImageFromUrl(it.imageResId, placeholder_image)
                placeholder_name.text = it.name
                placeholder_description.text = it.description



                placeholder_categories.layoutManager = layoutManager
                placeholder_tags.layoutManager = layoutManager2

                placeholder_note.text = it.note
                placeholder_categories.adapter = FlexAdaptor(it.C_categories.toMutableList())
                placeholder_tags.adapter = FlexAdaptor(it.tags.toMutableList())
            } ?: run {
                Log.d("Recipe", "No recipe found for the provided ID.")
            }
            checkRecipeOwnershipAndSetVisibility(
                recipeId = recipeId,
                currentUserUid = UserManager.getUser()!!.uid,
                button = deleteBtn
            )
            deleteBtn.setOnClickListener{
                deleteRecipeAndFollows(recipeId)
            }

        }
    }
    private fun checkIfUserIsOwner() {
        try {
            val currentUser = FirebaseAuth.getInstance().currentUser
            val recipeId = this.recipeId  // 상위에서 초기화되어 있다고 가정

            if (currentUser != null) {
                val db = FirebaseFirestore.getInstance()
                Log.d("Recipe_Owner_Check", "시작점까지 도달")

                db.collection("recipe")
                    .document(recipeId)
                    .get()
                    .addOnSuccessListener { recipeDoc ->
                        if (recipeDoc.exists()) {
                            val containedChannelName = recipeDoc.getString("contained_channel")
                            Log.d("Recipe_Owner_Check", "레시피에서 채널명 가져옴: $containedChannelName")

                            if (!containedChannelName.isNullOrEmpty()) {
                                // name으로 채널 문서 찾기
                                db.collection("channel")
                                    .whereEqualTo("name", containedChannelName)
                                    .get()
                                    .addOnSuccessListener { querySnapshot ->
                                        if (!querySnapshot.isEmpty) {
                                            val channelDoc = querySnapshot.documents[0]
                                            val ownerUid = channelDoc.getString("owner")

                                            if (ownerUid == currentUser.uid) {
                                                // 소유자일 경우 버튼 보이기
                                                deleteRecipeButton.visibility = View.VISIBLE
                                                editBtn.visibility = View.VISIBLE
                                                Log.d("Recipe_Owner_Check", "채널 소유자 확인 완료: ${currentUser.uid}")
                                            } else {
                                                // 소유자 아님
                                                deleteRecipeButton.visibility = View.GONE
                                                editBtn.visibility = View.GONE
                                                Log.d("Recipe_Owner_Check", "채널 소유자 아님: ${currentUser.uid}")
                                            }
                                        } else {
                                            Log.e("Firestore", "채널 문서 없음 (name = $containedChannelName)")
                                        }
                                    }
                                    .addOnFailureListener {
                                        Log.e("Firestore", "채널 문서 조회 실패", it)
                                    }
                            } else {
                                Log.e("Firestore", "contained_channel 값이 없음")
                            }
                        } else {
                            Log.e("Firestore", "레시피 문서 존재하지 않음")
                        }
                    }
                    .addOnFailureListener {
                        Log.e("Firestore", "레시피 문서 조회 실패", it)
                    }

            } else {
                Log.e("Recipe_Owner_Check", "로그인한 유저 없음")
            }

        } catch (error: Exception) {
            Log.e("Recipe_Owner_Check", "예외 발생: ${error.message}")
        }
    }
    fun deleteRecipeAndFollows(recipeId: String) {
        val db = FirebaseFirestore.getInstance()

        // 레시피 삭제
        db.collection("recipe").document(recipeId)
            .delete()
            .addOnSuccessListener {
                Log.d("Firestore", "레시피 삭제 완료")

                // 관련된 recipe_follow 문서들 삭제
                db.collection("recipe_follow")
                    .whereEqualTo("recipeID", recipeId)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        for (doc in querySnapshot.documents) {
                            db.collection("recipe_follow").document(doc.id).delete()
                        }
                        Log.d("Firestore", "관련된 recipe_follow 문서들 삭제 완료")
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "recipe_follow 삭제 실패: ${e.message}")
                    }

            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "레시피 삭제 실패: ${e.message}")
            }
    }


    // 레시피 소유자와 관련된 버튼 표시/숨기기
    fun checkRecipeOwnershipAndSetVisibility(
        recipeId: String,
        currentUserUid: String,
        button: View
    ) {
        val db = FirebaseFirestore.getInstance()

        // 레시피 문서 가져오기
        db.collection("recipe").document(recipeId)
            .get()
            .addOnSuccessListener { recipeDoc ->
                if (recipeDoc.exists()) {
                    val channelName = recipeDoc.getString("contained_channel")
                    if (!channelName.isNullOrEmpty()) {
                        // 채널 문서 가져오기
                        db.collection("channel").document(channelName)
                            .get()
                            .addOnSuccessListener { channelDoc ->
                                if (channelDoc.exists()) {
                                    val ownerUid = channelDoc.getString("owner")
                                    // 현재 UID와 비교
                                    if (ownerUid == currentUserUid) {
                                        // 버튼 보여주기
                                        button.visibility = View.VISIBLE
                                    }
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("Firestore", "채널 조회 실패: ${e.message}")
                            }
                    } else {
                        Log.e("Firestore", "contained_channel 정보 없음")
                    }
                } else {
                    Log.e("Firestore", "레시피 문서 없음")
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "레시피 조회 실패: ${e.message}")

        }
    }


    private fun estimateCaloriesWithAI(recipe: RecipeItem, onResult: (String) -> Unit) {
        val prompt = """
        다음은 레시피 정보입니다.
        - 재료: ${recipe.ingredients.joinToString(", ")}
        - 조리 과정: ${recipe.order}

        이 레시피를 기반으로 전체 예상 칼로리를 추정해 주세요.
        결과는 숫자와 단위만 출력하세요. 예: "350 kcal"
    """.trimIndent()
        Log.d("CALORIE_PROMPT", "재료: ${recipe.ingredients.joinToString()} | 과정: ${recipe.order}")
        Log.d("CALORIE_PROMPT", "재료 수: ${recipe.ingredients.size} / 조리 내용 길이: ${recipe.order.length}")

        val openAI = OpenAIClient()

        val apiKey = ApiKeyManager.getGptApi()

        if (apiKey == null) {
            Log.e("AI_ERROR", "API 키가 설정되지 않았습니다.")
            onResult("0 kcal")
            return
        }

        openAI.apiKeyInfo = apiKey

        openAI.sendMessage(
            prompt = prompt,
            role = "당신은 요리 레시피를 기반으로 칼로리를 정확하게 추정하는 AI입니다. 숫자와 단위만 출력하세요.",
            onSuccess = { response ->
                Log.d("AI_SUCCESS_RAW", "AI 원본 응답: '${response}'") // 홑따옴표로 감싸서 확인
                onResult(response.trim())
            },
            onError = { errorMsg ->
                Log.e("AI_ERROR", "AI 오류: $errorMsg")
                onResult("0 kcal")
            }
        )
    }

    private fun loadComments() {
        db.collection("recipe").document(recipeId) // 레시피 ID 기반으로 변경
            .collection("comments").orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("RecipeViewActivity", "댓글을 불러오는 중 오류 발생", e)
                    return@addSnapshotListener
                }
                val comments = snapshot?.documents?.mapNotNull { it.toObject(Comment::class.java) }
                comments?.let {
                    commentAdapter.updateComments(it)
                }
            }
    }


    private fun postComment(commentText: String) {
        val userId = UserManager.getUser()!!.uid;
        db.collection("user").document(userId).get().addOnSuccessListener { document ->
            if (document.exists()) {
                val userName = document.getString("name") ?: "익명"
                val userProfileImage = document.getString("image") ?: ""

                val comment = Comment(commentText, System.currentTimeMillis(), userId, userName, userProfileImage)

                db.collection("recipe").document(recipeId)
                    .collection("comments").add(comment)
                    .addOnSuccessListener {
                        Log.d("RecipeViewActivity", "댓글 저장 성공")
                    }
                    .addOnFailureListener { e ->
                        Log.w("RecipeViewActivity", "댓글 저장 실패", e)
                    }
            }
        }
    }
    fun deleteRecipe(recipeId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("recipe").document(recipeId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "레시피가 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_OK)
                finish() // 현재 액티비티 종료 (ex. 목록으로 돌아가기)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "삭제 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    // 댓글 데이터 모델
    data class Comment(
        val text: String = "",
        val timestamp: Long = 0L,
        val userId: String = "", // 사용자 ID 추가
        val userName: String = "", // 사용자 이름 추가
        val userProfileImage: String = "" // 사용자 프로필 사진 URL 추가
    )




    // Done 버튼 클릭 시 처리할 로직
    private fun onDoneButtonClick(position: Int) {
        // 현재 항목을 숨기고 다음 항목을 보이게 하는 로직을 구현합니다.
        val currentViewHolder =
            adaptorViewList.findViewHolderForAdapterPosition(position) as RecipeDetailRecyclerAdaptor.ViewHolder?

        // Done 버튼 숨기기
        currentViewHolder?.doneButton?.visibility = View.GONE

        // CheckBox 체크 상태 변경
        currentViewHolder?.checkBox?.isChecked = true

        // 배경색을 회색으로 흐리게 하기 (배경색 변경)
        currentViewHolder?.itemView?.setBackgroundColor(Color.parseColor("#D3D3D3"))  // 회색으로 배경 변경
        try {
            val item = items[position + 1]
            val regex = Regex("(.*)\\s*\\((.*),(\\d{2}:\\d{2}:\\d{2})\\)") // 타이머가 포함된 형식
            val matchResult = regex.find(item)
            val nextViewHolder =
                adaptorViewList.findViewHolderForAdapterPosition(position + 1) as RecipeDetailRecyclerAdaptor.ViewHolder?
            // 타이머가 존재하면, 다음 항목의 타이머를 보이게 설정
            if (matchResult != null) {
                nextViewHolder?.timerFrame?.visibility = View.VISIBLE
                val method = matchResult.groupValues[2].trim() // 조리방식 문자열
                val timeStr = matchResult.groupValues[3].trim() // "hh:mm:ss" 의 시간 비슷한 문자열
                nextViewHolder?.timerTitle?.text = method
                nextViewHolder?.timerTime?.text = timeStr
            } else {
                // 다음 항목 버튼 바로 보이기
                nextViewHolder?.doneButton?.visibility = View.VISIBLE
            }
        } catch (error: Exception) {
            Log.e("RecipeCooking", "Error Occured.. :${error.message}") // Index 오류일 것이다. 그대로 둔다.
        }

    }
}
class IngredientAdapter(
    private val ingredients: MutableList<String>,
    private val context: Context,
    private val onButtonClick: (String) -> Unit // 버튼 클릭 시 실행할 함수 전달
) : RecyclerView.Adapter<IngredientAdapter.IngredientViewHolder>() {

    class IngredientViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ingredientText: TextView = itemView.findViewById(R.id.textIngre)
        val ingredientButton: Button = itemView.findViewById(R.id.purchase)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IngredientViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_with_dots, parent, false)
        return IngredientViewHolder(view)
    }

    override fun onBindViewHolder(holder: IngredientViewHolder, position: Int) {
        val ingredient = ingredients[position]
        holder.ingredientText.text = ingredient
        holder.ingredientButton.visibility = View.GONE
        // 버튼 클릭 이벤트 추가
        holder.ingredientText.setOnClickListener{
            val encodedQuery = Uri.encode(ingredient) // 검색어 URL 인코딩
            val searchUrl = "https://search.shopping.naver.com/search/all?query=$encodedQuery"

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl))
            context.startActivity(intent)
        }
        holder.ingredientButton.setOnClickListener {
//            onButtonClick(ingredient) // 클릭 시 외부에서 전달된 함수 실행
//            val encodedQuery = Uri.encode(ingredient) // 검색어 URL 인코딩
//            val searchUrl = "https://search.shopping.naver.com/search/all?query=$encodedQuery"
//
//            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl))
//            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = ingredients.size

    // 새로운 아이템 추가 함수
    fun addItem(newItem: String) {
        ingredients.add(newItem)
        notifyItemInserted(ingredients.size - 1)
    }
}
// 댓글 RecyclerView 어댑터
class CommentAdapter(private var comments: MutableList<Comment>) :
    RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    inner class CommentViewHolder(holder: View) : RecyclerView.ViewHolder(holder) {
            val profileImage: ImageView = holder.findViewById(R.id.commentImage)
            val userName: TextView = holder.findViewById(R.id.commentAuthor)
            val commentText: TextView = holder.findViewById(R.id.commentText2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
            holder.userName.text = comment.userName
            holder.commentText.text = comment.text

            // 프로필 이미지 로드 (Glide 또는 FireStoreHelper 사용)
            if (comment.userProfileImage.isNotEmpty()) {
                FireStoreHelper.loadImageFromUrl(comment.userProfileImage, holder.profileImage)
            } else {
                holder.profileImage.setImageResource(R.drawable.dish_icon) // 기본 이미지
            }
    }

    override fun getItemCount(): Int = comments.size

    fun updateComments(newComments: List<Comment>) {
        comments.clear()
        comments.addAll(newComments)
        notifyDataSetChanged()
    }

}