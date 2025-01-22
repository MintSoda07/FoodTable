package com.bcu.foodtable

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.GridView
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.SearchView
import android.widget.TextView
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet.Layout
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.RecyclerView
import com.bcu.foodtable.databinding.ActivityHomeAcitivityBinding
import com.bcu.foodtable.useful.CategoryAdapter
import com.bcu.foodtable.useful.FireStoreHelper
import com.bcu.foodtable.useful.RecipeAdapter
import com.bcu.foodtable.useful.RecipeItem
import com.bcu.foodtable.useful.UsefulRecycler
import com.bcu.foodtable.useful.UserManager
import com.bcu.foodtable.useful.ViewAnimator
import com.google.android.material.navigation.NavigationView


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


    // 여기 아래로 다음 주석까지의 부분은 모두 임시로 지정된 데이터임. DB와 연결 시 수정해야 할 부분.
    private val dataListBig: MutableList<String> =
        mutableListOf("한식", "양식", "일식", "중식", "기타")
    private val dataListMed: MutableList<String> =
        mutableListOf("밥", "빵", "면", "국/찌개", "나물", "볶음", "구이", "찜", "튀김", "디저트", "음료", "기타")
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

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomeAcitivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_home_acitivity)

        homeSearchBarAppMenu = findViewById(R.id.appbar)
        homeSearchBar = findViewById(R.id.searchViewBar)
        contentScrollView = findViewById(R.id.scrollContentView)
        navView.setupWithNavController(navController)


        categoryMenuBar = findViewById(R.id.CategoryMenuBar)

        // 클릭 리스너
        CategoryAdapterBig = CategoryAdapter(
            dataListBig
        ) { item ->
            println("Clicked: $item")
        }
        CategoryAdapterMed = CategoryAdapter(
            dataListMed
        ) { item ->
            println("Clicked: $item")
        }
        CategoryAdapterSmall = CategoryAdapter(
            dataListSmall
        ) { item ->
            println("Clicked: $item")
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
        userName.text = userData.Name
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
}