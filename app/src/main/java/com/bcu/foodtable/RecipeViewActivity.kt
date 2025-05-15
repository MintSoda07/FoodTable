package com.bcu.foodtable

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bcu.foodtable.AI.OpenAIClient
import com.bcu.foodtable.R.id.likeCountText
import com.bcu.foodtable.RecipeViewActivity.Comment
import com.bcu.foodtable.ui.subscribeNavMenu.EditRecipeActivity
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
import android.Manifest
import com.bcu.foodtable.ui.home.RecommendManager
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.result.ActivityResultLauncher
import com.bcu.foodtable.Whisper.TextToSpeechProvider
import com.bcu.foodtable.Whisper.VoiceCookingManager
import java.util.Locale

class RecipeViewActivity : AppCompatActivity(), TextToSpeech.OnInitListener, TextToSpeechProvider {
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
    private lateinit var likeButton: ImageButton
    private val db = FirebaseFirestore.getInstance()
    private lateinit var pdfBtn: Button
    private lateinit var recipeItems: RecipeItem
    private lateinit var html: String

    private lateinit var micButton: ImageButton
    private var currentStepIndex = 0

    val aiUseCost = 50
    private var isClickedUpdated = false
    private lateinit var notificationPermissionManager: NotificationPermissionManager

    // TTS 및 음성 선언
    lateinit var voiceCookingManager: VoiceCookingManager
    private lateinit var tts: TextToSpeech
    // 타이머 버튼
    private lateinit var timerStartButton: Button
    // 타이머 어댑터 리스트 가져오기
    fun getAdaptorViewList(): RecyclerView = adaptorViewList
    fun getCurrentStepIndex(): Int = currentStepIndex
    fun setCurrentStepIndex(index: Int) {
        currentStepIndex = index
    }
    fun getRecipeAdaptor(): RecipeDetailRecyclerAdaptor = RecipeAdaptor
    //레시피 어댑터

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


        likeButton = findViewById(R.id.likeButton)
        val likeCountText = findViewById<TextView>(R.id.likeCountText)

        deleteRecipeButton = findViewById(R.id.deleteRecipeButton)
        editBtn = findViewById(R.id.editRecipeButton)
        // 레시피 수정 기능 (editBtn 클릭 시 수정 화면으로 이동)
        editBtn.setOnClickListener {
            val intent = Intent(this, EditRecipeActivity::class.java)
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
        // 타이머

        // Intent로 전달된 데이터 받기
        recipeId = intent.getStringExtra("recipe_id") ?: ""
        likeButton.setOnClickListener {
            val currentUserId = UserManager.getUser()?.uid
            if (currentUserId == null) {
                Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()  // 메시지 수정 가능
                return@setOnClickListener
            }

            val recipeCollection = FirebaseFirestore.getInstance().collection("recipe")
            val recipeDocRef = recipeCollection.document(recipeId)

            recipeDocRef
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val likes = document.getLong("likes") ?: 0
                        val likedUsers = document.get("likedUsers") as? List<String> ?: listOf()

                        val isLiked = likedUsers.contains(currentUserId)

                        val updatedLikes = if (isLiked) likes - 1 else likes + 1
                        val updatedLikedUsers = if (isLiked) {
                            likedUsers.filter { it != currentUserId }
                        } else {
                            likedUsers + currentUserId
                        }

                        // Firestore 업데이트
                        recipeDocRef
                            .update(
                                mapOf(
                                    "likes" to updatedLikes,
                                    "likedUsers" to updatedLikedUsers
                                )
                            )
                            .addOnSuccessListener {
                                Toast.makeText(this, if (isLiked) "좋아요 취소" else "좋아요!", Toast.LENGTH_SHORT).show()

                                //  좋아요 수 텍스트뷰 업데이트
                                likeCountText.text = updatedLikes.toString()

                                //  좋아요 아이콘 변경
                                likeButton.setImageResource(
                                    if (isLiked) R.drawable.likes_default else R.drawable.likes_filled
                                )
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "업데이트 실패: ${it.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this, "레시피를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "레시피 로드 실패: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }

        fun initLikeStatus() {
            val currentUserId = UserManager.getUser()?.uid ?: return
            val recipeDocRef = FirebaseFirestore.getInstance().collection("recipe").document(recipeId)

            recipeDocRef.get()
                .addOnSuccessListener { document ->
                    if (!document.exists()) return@addOnSuccessListener

                    val likes = document.getLong("likes") ?: 0
                    val likedUsers = document.get("likedUsers") as? List<String> ?: listOf()
                    val isLiked = likedUsers.contains(currentUserId)

                    // 좋아요 수와 아이콘 초기화
                    likeCountText.text = likes.toString()
                    likeButton.setImageResource(
                        if (isLiked) R.drawable.likes_filled else R.drawable.likes_default
                    )
                }
        }
        //initLikeStatus()


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
                    RecommendManager.recordRecipeHistory(it.id,it.C_categories)
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
                Log.d("Recipe_Edit_BtnCheck", "유저이름은 :" + UserManager.getUser()!!.uid)


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
                recipeItems = it


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
            deleteBtn.setOnClickListener {
                deleteRecipeAndFollows(recipeId)
            }


        }
        CoroutineScope(Dispatchers.Main).launch {
            val recipe = FirebaseHelper.getDocumentById("recipe", recipeId, RecipeItem::class.java)

            recipe?.let {
                recipeItems = it
                val html = """
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <style>
        @font-face {
            font-family: 'NotoSerifKR';
            font-style: normal;
            font-weight: 400;
            src: local('Noto Serif KR'), url('https://fonts.googleapis.com/css2?family=Noto+Serif+KR&display=swap');
        }

        body {
            font-family: 'NotoSerifKR', serif;
            background-color: #f8f4ec;
            color: #333;
            padding: 40px;
        }

        .recipe-container {
            max-width: 700px;
            margin: auto;
            background-color: white;
            padding: 40px;
            border-radius: 20px;
            box-shadow: 0 0 20px rgba(0,0,0,0.1);
        }

        .recipe-title {
            font-size: 36px;
            color: #a0522d;
            text-align: center;
            margin-bottom: 10px;
        }

        .recipe-subtitle {
            text-align: center;
            color: #888;
            font-size: 16px;
            margin-bottom: 30px;
        }

        .recipe-image {
            width: 100%;
            height: auto;
            border-radius: 10px;
            margin-bottom: 30px;
        }

        .section-title {
            font-size: 20px;
            margin-top: 30px;
            color: #444;
            border-bottom: 1px solid #ccc;
            padding-bottom: 5px;
        }

        .description, .note, .order, .ingredients, .tags {
            font-size: 15px;
            line-height: 1.8;
            margin-top: 10px;
        }

        ul {
            margin: 0;
            padding-left: 20px;
        }

        .footer {
            margin-top: 40px;
            text-align: right;
            font-size: 12px;
            color: #888;
        }
        .step {
            border-left: 4px solid #a0522d;
            background-color: #fffaf4;
            padding: 15px 20px;
            margin: 20px 0;
            border-radius: 10px;
            box-shadow: 0 0 5px rgba(0,0,0,0.05);
        }

        .step-header {
            font-weight: bold;
            font-size: 18px;
            margin-bottom: 8px;
            color: #5a3e2b;
        }

        .step-number {
            background-color: #a0522d;
            color: white;
            padding: 2px 8px;
            border-radius: 5px;
            margin-right: 10px;
        }

        .step-title {
            font-style: italic;
        }

        .step-description {
            font-size: 15px;
            color: #333;
            line-height: 1.6;
            margin-bottom: 5px;
        }

        .step-meta {
            font-size: 13px;
            color: #777;
            font-style: italic;
            margin-top: 5px;
        }

        .method {
            color: #444;
        }

        .duration {
            color: #444;
        }
        /* 각 레시피 블록 단위로 잘리지 않게 설정 */
  .recipe-container {
    page-break-inside: avoid;  /* 페이지 내부에서 분리 금지 */
    break-inside: avoid;
  }

  /* 전체 컨테이너도 부드럽게 나눔 */
  body {
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }
    </style>
</head>
<body>
    <div class="recipe-container">
        <div class="recipe-title">${recipeItems.name}</div>
        <div class="recipe-subtitle">열람수: ${recipeItems.clicked}회 • 예상 열량: ${recipeItems.estimatedCalories ?: "정보 없음"}</div>
        <img class="recipe-image" src="${recipeItems.imageResId}" alt="레시피 이미지">

        <div class="section-title">설명</div>
        <div class="description">${recipeItems.description}</div>
        <br><br><br>

        <div class="section-title">조리 순서</div>
        <div class="order">${parseOrderSteps(recipeItems.order)}</div>

        <div class="section-title">재료</div>
        <div class="ingredients">
            <ul>
                ${recipeItems.ingredients.joinToString("\n") { "<li>$it</li>" }}
            </ul>
        </div>

        <div class="section-title">카테고리</div>
        <div class="tags">${recipeItems.C_categories.joinToString(", ")}</div>

        <div class="section-title">태그</div>
        <div class="tags">${recipeItems.tags.joinToString(", ")}</div>

        ${
                    if (recipeItems.note.isNotBlank()) """
        <div class="section-title">비고</div>
        <div class="note">${recipeItems.note}</div>
        """ else ""
                }

        <div class="footer">채널: ${recipeItems.contained_channel} • 날짜: ${recipeItems.date.toDate()}</div>
    </div>
</body>
</html>
""".trimIndent()
                Log.i("PDF Provider","Recipe  내용${recipeItems} \n \n 그리고 html 내용 ${html}")
                pdfBtn = findViewById(R.id.pdfPrintbtn)
                pdfBtn.setOnClickListener {
                    var userData = UserManager.getUser()!!
                    if ( userData.point < aiUseCost) {
                        Toast.makeText(
                            applicationContext,
                            "소금이 부족합니다. PDF 사용 기능은 50소금이 필요합니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }
                    // 소금 계산을 위해 유저 설정 불러오기
                    Toast.makeText(
                        applicationContext,
                        "50 소금을 사용하여 PDF를 생성합니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                    userData.point -= 50
                    CoroutineScope(Dispatchers.IO).launch {
                        UserManager.setUserByDatatype(userData)
                        updateFieldById(
                            collectionPath = "user",
                            documentId = userData.uid,
                            fieldName = "point",
                            newValue = userData.point
                        )
                    }
                    createPdfFromHtml(this@RecipeViewActivity, html, "레시피_${recipeItems.name}")
                }
            }
        }
        tts = TextToSpeech(this, this)
        // 음성 연결
        // 1. VoiceCookingManager 초기화
        voiceCookingManager = VoiceCookingManager(
            context = this,
            micButton = findViewById(R.id.micButton),
            onNextStep = {
                // 1. 현재 ViewHolder 가져오기
                val currentViewHolder =
                    getAdaptorViewList().findViewHolderForAdapterPosition(currentStepIndex)
                            as? RecipeDetailRecyclerAdaptor.ViewHolder

                // 2. 현재 타이머 종료 및 UI 숨기기
                currentViewHolder?.timer?.cancel()
                currentViewHolder?.isTimerRunning = false
                currentViewHolder?.timerFrame?.visibility = View.GONE
                currentViewHolder?.startButton?.visibility = View.GONE
                currentViewHolder?.stopButton?.visibility = View.GONE
                currentViewHolder?.skipButton?.visibility = View.GONE

                // 3. 완료 체크 및 회색 배경 처리
                currentViewHolder?.doneButton?.visibility = View.GONE
                currentViewHolder?.checkBox?.isChecked = true
                currentViewHolder?.itemView?.setBackgroundColor(Color.parseColor("#D3D3D3"))

                // 4. 다음 단계로 이동
                onDoneButtonClick(currentStepIndex)
                currentStepIndex += 1
            },
            onRepeat = { repeatStep() },
            onStop = { stopCooking() },
            onStartTimer = {
                timerStartButton.performClick()  // ← 음성으로 타이머 시작
            }
        )




        // 2. 마이크 버튼 누르면 시작
        micButton = findViewById(R.id.micButton)
        micButton.setOnClickListener {
            if (voiceCookingManager.isRunning()) {
                voiceCookingManager.stop()
                Toast.makeText(this, "음성 인식을 종료합니다.", Toast.LENGTH_SHORT).show()
            } else {
                // 현재 체크된 단계까지 검사해서 currentStepIndex 업데이트
                val currentIndex = RecipeAdaptor.getCurrentStepIndex()
                setCurrentStepIndex(currentIndex)

                voiceCookingManager.start()
                Toast.makeText(
                    this,
                    "음성 인식을 시작합니다. '다음', '반복', '타이머' 등을 말해보세요.",
                    Toast.LENGTH_SHORT
                ).show()
                // 0단계일 경우에만 안내 메시지 출력
                if (currentIndex == 0) {
                    speakStep(0)
                }
            }
        }






    }

    //TTS 코드 초기화 함수
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.KOREAN
        }
    }
    // 조리 단계 읽어 주는 함수
    fun speakStep(position: Int) {
        Log.d("VoiceCooking", "실제 speakStep 내부 position: $position")
        if (position >= items.size) return

        val step = items[position].trim()

        // 정규식으로 파싱 (예: "1. (준비하기) 오븐을 예열하세요.")
        val regex = Regex("""\d+\.\s*\((.*?)\)\s*(.*)""")
        val match = regex.find(step)

        val speechText = if (match != null) {
            val title = match.groupValues[1]  // "준비하기"
            val description = match.groupValues[2]  // "오븐을 예열하세요."

            if (position == 0) {
                "요리를 시작하겠습니다. 첫 번째 단계입니다. ${title} 단계: ${description} 해주세요."
            } else {
                "이제 ${title} 단계입니다. ${description} 해주세요."
            }

        } else {
            // 예외 상황: 정규식에 맞지 않으면 전체 문장 그대로
            if (position == 0) {
                "요리를 시작하겠습니다. 첫 번째 단계입니다. ${step}"
            } else {
                step
            }
        }

        tts.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, "STEP_$position")
    }




    override fun onDestroy() {
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }
    override fun onPause() {
        if (voiceCookingManager.isRunning()) {
            voiceCookingManager.stop()
            Log.d("VoiceCooking", "음성 인식 onPause()에서 종료됨")
        }
        super.onPause()
    }

    override fun getTTS(): TextToSpeech = tts

    private fun repeatStep() {
        // 현재 단계를 다시 실행하는 로직 (체크 상태 풀고 다시 실행 등)
        val index = currentStepIndex - 1
        if (index >= 0) {
            speakStep(index) // 반복해서 TTS로 읽어주기
            onDoneButtonClick(index)
        }
    }

    private fun stopCooking() {
        Toast.makeText(this, "요리를 종료합니다.", Toast.LENGTH_SHORT).show()
        // 버튼 상태 초기화, TTS, 기타 리셋 작업
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
                                                Log.d(
                                                    "Recipe_Owner_Check",
                                                    "채널 소유자 확인 완료: ${currentUser.uid}"
                                                )
                                            } else {
                                                // 소유자 아님
                                                deleteRecipeButton.visibility = View.GONE
                                                editBtn.visibility = View.GONE
                                                Log.d(
                                                    "Recipe_Owner_Check",
                                                    "채널 소유자 아님: ${currentUser.uid}"
                                                )
                                            }
                                        } else {
                                            Log.e(
                                                "Firestore",
                                                "채널 문서 없음 (name = $containedChannelName)"
                                            )
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
        Log.d(
            "CALORIE_PROMPT",
            "재료 수: ${recipe.ingredients.size} / 조리 내용 길이: ${recipe.order.length}"
        )

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
    // 음성 마이크 권한 추가
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "마이크 권한이 허용되었습니다.", Toast.LENGTH_SHORT).show()
                //startWhisperRecording()
            } else {
                Toast.makeText(this, "마이크 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }





    private fun postComment(commentText: String) {
        val userId = UserManager.getUser()!!.uid;
        db.collection("user").document(userId).get().addOnSuccessListener { document ->
            if (document.exists()) {
                val userName = document.getString("name") ?: "익명"
                val userProfileImage = document.getString("image") ?: ""

                val comment = Comment(
                    commentText,
                    System.currentTimeMillis(),
                    userId,
                    userName,
                    userProfileImage
                )

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

        //  현재 타이머가 있으면 정지 및 숨김 처리
        currentViewHolder?.timer?.cancel()
        currentViewHolder?.isTimerRunning = false
        currentViewHolder?.timerFrame?.visibility = View.GONE
        currentViewHolder?.startButton?.visibility = View.GONE
        currentViewHolder?.stopButton?.visibility = View.GONE
        currentViewHolder?.skipButton?.visibility = View.GONE

        //  체크박스 및 배경 변경
        currentViewHolder?.checkBox?.isChecked = true
        currentViewHolder?.doneButton?.visibility = View.GONE
        currentViewHolder?.itemView?.setBackgroundColor(Color.parseColor("#D3D3D3"))
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
            if (voiceCookingManager.isRunning()) {
                speakStep(position + 1)
            }
        } catch (error: Exception) {
            Log.e("RecipeCooking", "Error Occured.. :${error.message}") // Index 오류일 것이다. 그대로 둔다.
        }
        if (position + 1 >= items.size) {
            // 마지막 단계까지 완료한 경우
            // Toast.makeText(this, "모든 조리 과정이 완료되었습니다.", Toast.LENGTH_SHORT).show()

            if (voiceCookingManager.isRunning()) {
                voiceCookingManager.stop()
                val tts = getTTS()
                tts.speak("모든 조리 과정이 완료되었습니다. 수고하셨습니다.", TextToSpeech.QUEUE_FLUSH, null, "COOK_DONE")
            }
        }

    }

    // PDF 출력용 HTML 생성부 및 PDF 출력부
    //createPdfFromHtml(this, html, "my_recipe") 호출부는 상단 OnCreate에 있음
    // suspend 함수로 변경
    fun parseOrderSteps(order: String): String {
        val steps = order.split("○").filter { it.isNotBlank() }

        return steps.map { step ->
            val regex = Regex("""(\d+)\.\s*\((.*?)\)\s*(.*?)(\(([^,]+),([^)]+)\))?$""")
            val match = regex.find(step.trim())

            if (match != null) {
                val number = match.groupValues[1]
                val title = match.groupValues[2]
                val description = match.groupValues[3]
                val method = match.groupValues.getOrNull(5)?.trim() ?: ""
                val duration = match.groupValues.getOrNull(6)?.trim() ?: ""

                """
            <div class="step">
                <div class="step-header">
                    <span class="step-number">$number.</span>
                    <span class="step-title">$title</span>
                </div>
                <div class="step-description">$description</div>
                ${
                    if (method.isNotEmpty() && duration.isNotEmpty()) """
                <div class="step-meta">
                    <span class="method">⏱ $method</span> |
                    <span class="duration">$duration</span>
                </div>""" else ""
                }
            </div>
            """
            } else {
                "<div class=\"step\"><div class=\"step-description\">${step.trim()}</div></div>"
            }
        }.joinToString("\n")
    }


    @SuppressLint("SetJavaScriptEnabled")
    fun createPdfFromHtml(activity: Activity, htmlContent: String, fileName: String) {
        // WebView 생성 및 설정
        val webView = WebView(activity)
        webView.settings.javaScriptEnabled = false
        webView.setBackgroundColor(Color.WHITE) // PDF 배경 흰색 보장
        webView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // WebView를 보이지 않게 FrameLayout에 추가 (실제 렌더링을 위해 필요)
        val container = FrameLayout(activity)
        container.addView(webView)
        activity.addContentView(container, ViewGroup.LayoutParams(0, 0)) // 보이지 않게 추가

        // HTML 로딩
        webView.loadDataWithBaseURL("file:///android_asset/", htmlContent, "text/html", "UTF-8", null)


        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                // 렌더링 지연을 위한 postDelayed
                Handler(Looper.getMainLooper()).postDelayed({
                    createPdfFromWebView(view, fileName)
                }, 500)
            }
        }
    }

    fun createPdfFromWebView(webView: WebView, fileName: String) {
        val printManager = webView.context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val printAdapter = webView.createPrintDocumentAdapter(fileName)
        val jobName = "$fileName Document"

        val builder = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setResolution(PrintAttributes.Resolution("pdf", "pdf", 600, 600))
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)

        val printJob = printManager.print(jobName, printAdapter, builder.build())
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
        holder.ingredientText.setOnClickListener {
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