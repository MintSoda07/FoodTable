package com.bcu.foodtable

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.GridView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.SearchView
import android.widget.TextView
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.RecyclerView
import com.bcu.foodtable.databinding.ActivityHomeAcitivityBinding
import com.bcu.foodtable.ui.home.HomeFragment
import com.bcu.foodtable.ui.home.HomeViewModel
import com.bcu.foodtable.useful.*
import com.google.android.flexbox.FlexboxLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.toObject
import androidx.activity.viewModels

class HomeAcitivity : AppCompatActivity() {


    lateinit var contentScrollView: ScrollView
    lateinit var homeSearchBar: SearchView
    lateinit var homeSearchBarAppMenu: View
    lateinit var categoryMenuBar: View

    lateinit var CategoryAdapterBig: CategoryAdapter
    lateinit var CategoryAdapterMed: CategoryAdapter
    lateinit var CategoryAdapterSmall: CategoryAdapter

    private var searchBarHidden = false
    private var categoryBarHidden = true

    private val viewModel: HomeViewModel by viewModels()


    private lateinit var tagContainer: FlexboxLayout  //  태그를 담을 뷰



    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()  // Firestore 초기화


    private var selectedCategory: String? = null  // 현재 선택한 카테고리 (종류, 조리방식, 재료)
    private var selectedFoodType: String? = null  // "종류" 선택값 (한 개만)
    private var selectedCookingMethod: String? = null  // "조리방식" 선택값 (한 개만)
    private val selectedIngredients = mutableSetOf<String>()  // "재료" 선택값 (여러 개 가능)


    // 여기 아래로 다음 주석까지의 부분은 모두 임시로 지정된 데이터임. DB와 연결 시 수정해야 할 부분.
    private val dataListBig: MutableList<String> =
        mutableListOf("종류", "조리방식", "재료")
    private val dataListMed: MutableList<String> =
        mutableListOf()
        private val dataListSmall: MutableList<String> =
        mutableListOf("단맛", "짠맛", "신맛", "쓴맛", "감칠맛", "매운맛", "기타")


    // 여기까지.

    private val hideSearchBarDelay = 5000L // 5초
    private val handler = Handler(Looper.getMainLooper())
    private var hideSearchBarRunnable: Runnable? = null


    private lateinit var recyclerViewSearchBig: RecyclerView
    private lateinit var recyclerViewSearchMed: RecyclerView
    private lateinit var recyclerViewSearchSmall: RecyclerView

    private lateinit var binding: ActivityHomeAcitivityBinding

    private  lateinit var recipeAdapter: RecipeAdapter
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomeAcitivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_home_acitivity)

        //  ExpandedGridView 가져오기
        val gridView = findViewById<ExpandedGridView>(R.id.cardGridView)

        //  Adapter 초기화 및 설정
        recipeAdapter = RecipeAdapter(this, mutableListOf())

        gridView.adapter = recipeAdapter

        viewModel.recipes.observe(this) { recipes ->
            Log.d("HomeActivity", "🔥 HomeActivity에서 레시피 업데이트: ${recipes.size}개")
            recipeAdapter.updateRecipes(recipes) // 레시피 목록 즉시 반영
        }

        // 🔥 앱이 실행될 때 즉시 데이터 로드
        if (viewModel.recipes.value.isNullOrEmpty()) {
            Log.d("HomeActivity", "📢 레시피가 비어있음 -> 강제 로드 실행")
            viewModel.loadRecipes() // 데이터 로드 실행
        }
        //  SearchView  설정
        setupSearchView()
        val query = intent.getStringExtra("SEARCH_QUERY") ?: ""
        if (query.isNotBlank()) {
            Log.d("HomeActivity", " 검색 실행: $query")
            searchRecipes(query)
        }


        // 버튼 클릭 시 fragment_mypage로 이동
        val myPageBtn = findViewById<ImageButton>(R.id.UserImageView)
        myPageBtn.setOnClickListener {
            navView.selectedItemId = R.id.navigation_mypage
        }

        homeSearchBarAppMenu = findViewById(R.id.appbar)
        homeSearchBar = findViewById(R.id.searchViewBar)
        contentScrollView = findViewById(R.id.scrollContentView)
        navView.setupWithNavController(navController)


        categoryMenuBar = findViewById(R.id.CategoryMenuBar)

        tagContainer = findViewById(R.id.tagContainer) //태그 컨테이너 초기화
        // 클릭 리스너
        CategoryAdapterBig = CategoryAdapter(
            dataListBig
        ) { selectedCategory ->
            fetchCategoryData(selectedCategory) //Firebase 데이터 불러오기
            println("Clicked: $selectedCategory")
        }
        CategoryAdapterMed = CategoryAdapter(
            dataListMed
        ) { item ->
            handleCategorySelection(item)
            println("Clicked: $item")
        }
        CategoryAdapterSmall = CategoryAdapter(
            dataListSmall
        ) { item ->
            println("Clicked: $item")
        }



        // 버튼 클릭 시 해당 카테고리를 설정하고 데이터를 불러옴
        CategoryAdapterBig = CategoryAdapter(
            mutableListOf("종류", "조리방식", "재료")
        ) { category ->
            selectedCategory = category // 현재 선택된 카테고리 설정
            fetchCategoryData(category) // Firestore에서 데이터 불러오기
            Log.d("CATEGORY_SELECTED", "현재 선택된 카테고리: $category")
        }

        selectedCategory = "종류"
        fetchCategoryData("종류")
        //환경설정 버튼 찾기
        val setting_Btn = findViewById<ImageButton>(R.id.setting_btn)

        //환경설정 페이지 이동
        setting_Btn.setOnClickListener {
            s_btn() // 클릭하면 s_btn() 실행
        }

        // 리사이클러 뷰
        recyclerViewSearchBig = findViewById(R.id.RecyclerViewCategoryBig)
        recyclerViewSearchMed = findViewById(R.id.RecyclerViewCategoryMed)
        recyclerViewSearchSmall = findViewById(R.id.RecyclerViewCategorySmall)

        UsefulRecycler.setupRecyclerView(recyclerViewSearchBig, CategoryAdapterBig, this, 1)
        UsefulRecycler.setupRecyclerView(recyclerViewSearchMed, CategoryAdapterMed, this, 2)
        UsefulRecycler.setupRecyclerView(recyclerViewSearchSmall, CategoryAdapterSmall, this, 1)
        
        
        val userName = findViewById<TextView>(R.id.placeholder_name)
        val userPoint = findViewById<TextView>(R.id.salt_placeholder)
        val userImage = findViewById<ImageView>(R.id.UserImageView)

        // 네비바 아이템 클릭시 약간의 진동
        val vibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val navBottom = findViewById<BottomNavigationView>(R.id.nav_view)
        navBottom.setOnClickListener{
            hideSearchView()
            vibrator.vibrate(250)
        }
        
        // 유저 설정 불러오기
        val userData = UserManager.getUser()!!
        userName.text = userData.name
        userPoint.text = userData.point.toString() + getString(R.string.title_salt)
        FireStoreHelper.loadImageFromUrl(userData.image,userImage)

        categoryMenuBar.visibility = View.INVISIBLE

        // 스크롤 뷰 터치 리스너
        contentScrollView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_MOVE -> {
                    checkScrollPosition()
                }
            }
            false
        }
        // 타이머 초기화 및 검색창 숨김 로직 설정
        // Runnable로 설정한다
        hideSearchBarRunnable = Runnable {
            if (!searchBarHidden&&contentScrollView.scrollY>10) hideSearchView() // 10 이상 스크롤 되지 않은 상태에 검색창이 있다면 -> 검색창을 숨긴다
        }

        // 검색창에 focus를 받았을 경우
        homeSearchBar.setOnQueryTextFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                showCategories()
            }
        }
        Log.d("Home_Activity","LOGGED IN WITH ${FirebaseAuth.getInstance().currentUser}")

    }

    // 환결설정 이동 함수
    fun s_btn(){
        ActivityTransition.startStatic(
            this@HomeAcitivity,
            Setting::class.java
        )
    }

    // 화면 클릭시 발생하는 이벤트를 재정의
    // - 특정 범위 클릭을 감지하는데 사용하였고, 검색창이나 카테고리창을 터치하였는지, 아니면 바깥쪽 영역을 터치하였는지 감지하는데 사용하였음.
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) { //터치 액션 중 누를 때 발동됨
            val currentFocusView = currentFocus
            if (currentFocusView != null) {
                // Rect() = 사각형 크기 (영역 크기 계산용)
                val outRect = Rect()
                val searchRect = Rect()
                val categoryRect = Rect()

                // View의 Rect() 크기를 정의해 준다.
                currentFocusView.getGlobalVisibleRect(outRect)
                homeSearchBar.getGlobalVisibleRect(searchRect)
                categoryMenuBar.getGlobalVisibleRect(categoryRect)

                // 검색창 & 카테고리 창 밖을 터치하면 카테고리를 숨기고, 포커스 해제와 함께 입력창을 닫는다.
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt()) &&
                    (!searchRect.contains(event.rawX.toInt(), event.rawY.toInt())) &&
                    (!categoryRect.contains(event.rawX.toInt(), event.rawY.toInt()))
                ) {
                    currentFocusView.clearFocus()
                    hideCategories()
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(currentFocusView.windowToken, 0)
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    // 스크롤 위취를 감지한다.
    private fun checkScrollPosition() {
        if (contentScrollView.scrollY <= 5) { // 5 이상 스크롤되지 않으면 (사실상 맨 위로 당기면)
            if (searchBarHidden) {
                showSearchView() //검색창을 표시한다.
            }
        }
        resetHideSearchBarTimer() // 검색창 타이머 초기화 ( 검색창이 위에 있지 않으면 5초 후 사라지는 함수 호출 )
    }
    // 카테고리 가져오기
    private fun fetchCategoryData(category: String) {
        val documentPath = when (category) {
            "종류" -> "C_food_types"
            "조리방식" -> "C_cooking_methods"
            "재료" -> "C_ingredients"
            else -> return
        }

        //  1. 버튼을 누르면 즉시 RecyclerView를 갱신하여 반응 속도 향상
        dataListMed.clear()  // 기존 데이터 삭제
        CategoryAdapterMed.notifyDataSetChanged()  // 즉시 UI 반영 (빈 화면이 표시됨)

        //  2. Firestore에서 데이터를 빠르게 가져오기
        db.collection("C_categories").document(documentPath)
            .get(Source.CACHE)  //  캐시에서 먼저 가져옴 (더 빠름)
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val list = document.get("list") as? List<String>
                    if (list != null) {
                        dataListMed.addAll(list)
                        CategoryAdapterMed.notifyDataSetChanged() // UI 업데이트
                    }
                }
            }
            .addOnFailureListener {
                //  캐시에서 가져오기 실패 시 네트워크에서 다시 가져옴
                db.collection("C_categories").document(documentPath)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            val list = document.get("list") as? List<String>
                            if (list != null) {
                                dataListMed.addAll(list)
                                CategoryAdapterMed.notifyDataSetChanged()
                            }
                        }
                    }
            }
    }




    // Firestore에서 최신 데이터 가져오기
    private fun fetchFromFirestore(documentPath: String) {
        db.collection("C_categories").document(documentPath)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val list = document.get("list") as? List<String>
                    if (list != null) {
                        updateCategoryList(list)
                    }
                }
            }
    }
    private fun updateCategoryList(newData: List<String>) {
        runOnUiThread {  //  UI 업데이트를 메인 스레드에서 실행
            dataListMed.clear()
            dataListMed.addAll(newData)
            CategoryAdapterMed.notifyDataSetChanged()
        }
    }

    // 카테고리를 표시한다
    private fun showCategories() {
        categoryMenuBar.visibility = View.VISIBLE
        ViewAnimator.moveYPos(categoryMenuBar, -600f, 0f, 300, DecelerateInterpolator(2f)) {
            categoryMenuBar.isClickable = true
        }.start()
        categoryBarHidden = false
    }

    // 카테고리를 숨긴다
    private fun hideCategories() {
        ViewAnimator.moveYPos(
            categoryMenuBar,
            0f,
            -600f,
            200,
            AccelerateInterpolator(1.2f)
        ) {
            categoryMenuBar.visibility = View.INVISIBLE;categoryMenuBar.isClickable =
            false
        }.start()
        categoryBarHidden = true
    }
    
    // 5초 후 검색창을 숨기는 기능
    private fun resetHideSearchBarTimer() {
        // 기존 타이머 취소
        hideSearchBarRunnable?.let { handler.removeCallbacks(it) }
        // 새로운 타이머 설정
        handler.postDelayed(hideSearchBarRunnable!!, hideSearchBarDelay)
    }

    // 검색창을 표시하는 기능
    private fun showSearchView() {
        homeSearchBarAppMenu.visibility = View.VISIBLE
        ViewAnimator.moveYPos(homeSearchBarAppMenu, -130f, 0f, 300, DecelerateInterpolator(2f)) {
            homeSearchBarAppMenu.isClickable = true
        }.start()
        ViewAnimator.moveYPos(contentScrollView, -130f, 0f, 300, DecelerateInterpolator(2f)).start()
        homeSearchBar.isFocusable = true
        homeSearchBar.isFocusableInTouchMode = true
        searchBarHidden = false
    }

    // 검색창을 숨기는 기능
    private fun hideSearchView() {
        ViewAnimator.moveYPos(
            homeSearchBarAppMenu,
            0f,
            -130f,
            300,
            AccelerateInterpolator(2f)
        ) {
            homeSearchBarAppMenu.visibility = View.INVISIBLE;homeSearchBarAppMenu.isClickable =
            false
        }.start()
        ViewAnimator.moveYPos(
            contentScrollView,
            0f,
            -130f,
            300,
            AccelerateInterpolator(2f)
        ).start()
        homeSearchBar.clearFocus()
        if(!categoryBarHidden) hideCategories()
        searchBarHidden = true
    }
    // 선택한 항목을 태그에 추가


    private fun handleCategorySelection(selectedItem: String) {
        Log.d("CATEGORY_SELECTION_BEFORE", "Category: $selectedCategory, FoodType: $selectedFoodType, CookingMethod: $selectedCookingMethod, Ingredients: $selectedIngredients")

        when (selectedCategory) {
            "종류" -> {
                if (selectedFoodType == selectedItem) {
                    selectedFoodType = null
                } else {
                    selectedFoodType = selectedItem
                }
            }

            "조리방식" -> {
                if (selectedCookingMethod == selectedItem) {
                    selectedCookingMethod = null
                } else {
                    selectedCookingMethod = selectedItem
                }
            }

            "재료" -> {
                if (selectedIngredients.contains(selectedItem)) {
                    selectedIngredients.remove(selectedItem)
                } else {
                    selectedIngredients.add(selectedItem)
                }
            }
        }

        Log.d("CATEGORY_SELECTION_AFTER", "FoodType: $selectedFoodType, CookingMethod: $selectedCookingMethod, Ingredients: $selectedIngredients")

        updateTagDisplay()  // 태그 UI 업데이트

        //  RecyclerView 전체 갱신 대신 변경된 항목만 업데이트
        recyclerViewSearchMed.adapter?.notifyItemChanged(dataListMed.indexOf(selectedItem))
    }










    private fun updateTagDisplay() {
        if (!::tagContainer.isInitialized) {
            Log.e("UI_ERROR", "태그 컨테이너가 초기화되지 않음")
            return
        }

        runOnUiThread {
            tagContainer.removeAllViews()  // 기존 태그 삭제

            Log.d("TAG_UPDATE", "FoodType: $selectedFoodType, CookingMethod: $selectedCookingMethod, Ingredients: $selectedIngredients")

            val selectedTags = mutableListOf<String>()

            // "종류" 태그 추가 (한 개만)
            selectedFoodType?.let { selectedTags.add("$it") }

            // "조리방식" 태그 추가 (한 개만)
            selectedCookingMethod?.let { selectedTags.add("$it") }

            // "재료" 태그 추가 (여러 개 가능)
            selectedTags.addAll(selectedIngredients.map { "$it" })  // 태그 형태로 저장

            Log.d("TAG_UPDATE_FINAL", "Selected Tags: $selectedTags")

            // UI에 태그 표시
            selectedTags.forEach { tag ->
                addTagToContainer(tag)
            }
        }
    }

    //  태그를 `FlexboxLayout`에 추가하는 함수

    private fun addTagToContainer(tagText: String) {
        val tagView = TextView(this).apply {
            text = "#$tagText"
            setPadding(16, 8, 16, 8)
            setBackgroundResource(R.drawable.tag_background)  // 태그 스타일 추가 필요
            setTextColor(Color.WHITE)
            textSize = 14f
            layoutParams = FlexboxLayout.LayoutParams(
                FlexboxLayout.LayoutParams.WRAP_CONTENT,
                FlexboxLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(8, 8, 8, 8)
            }
            setOnClickListener {
                removeTag(tagText)
            }
        }

        tagContainer.addView(tagView)
    }



    //  태그 제거 함수
    private fun removeTag(tagText: String) {
        Log.d("TAG_REMOVE_BEFORE", "FoodType: $selectedFoodType, CookingMethod: $selectedCookingMethod, Ingredients: $selectedIngredients")

        when {
            // "종류" 삭제
            selectedFoodType == tagText -> selectedFoodType = null

            // "조리방식" 삭제
            selectedCookingMethod == tagText -> selectedCookingMethod = null

            // "재료" 삭제
            selectedIngredients.contains(tagText) -> {
                selectedIngredients.remove(tagText)
            }
        }

        Log.d("TAG_REMOVE_AFTER", "FoodType: $selectedFoodType, CookingMethod: $selectedCookingMethod, Ingredients: $selectedIngredients")

        updateTagDisplay() // UI 업데이트
    }

    private fun searchRecipes(query: String) {
        db.collection("recipe")
            .get() //  Firestore에서 모든 레시피 데이터를 가져옴
            .addOnSuccessListener { documents ->
                val recipeList = mutableListOf<RecipeItem>()
                for (document in documents) {
                    val recipe = document.toObject(RecipeItem::class.java)

                    //  `name` 필드를 개별 단어로 분리
                    val words = splitWords(recipe.name)

                    //  검색어(query)가 단어 리스트에 포함되면 결과 리스트에 추가
                    if (words.any { it.contains(query, ignoreCase = true) }) {
                        recipeList.add(recipe)
                    }
                }
                recipeAdapter.updateRecipes(recipeList) //  UI 업데이트
            }
            .addOnFailureListener { exception ->
                Log.e("FirestoreSearch", "검색 중 오류 발생: ", exception)
            }
    }

    private fun splitWords(name: String): List<String> {
        return name.split(" ", "-", "_") //  띄어쓰기, 하이픈(-), 밑줄(_) 기준으로 단어 분리
    }

    private fun setupSearchView() {
        binding.searchViewBar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) { //  null 또는 빈 문자열 방지
                    val intent = Intent(this@HomeAcitivity, SearchResultActivity::class.java)
                    intent.putExtra("SEARCH_QUERY", query)
                    startActivity(intent)
                } else {
                    Log.e("HomeActivity", "검색어가 비어 있음")
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
    }


    private fun updateRecyclerView(recipeList: List<RecipeItem>) {
        recipeAdapter.updateRecipes(recipeList) // Adapter에 새 데이터 적용
    }





}