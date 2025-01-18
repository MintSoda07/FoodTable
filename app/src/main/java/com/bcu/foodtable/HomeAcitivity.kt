package com.bcu.foodtable

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.bcu.foodtable.useful.RecipeAdapter
import com.bcu.foodtable.useful.RecipeItem
import com.bcu.foodtable.useful.UsefulRecycler
import com.bcu.foodtable.useful.UserManager
import com.bcu.foodtable.useful.ViewAnimator


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
        mutableListOf("한식", "양식", "일식", "중식", "기타", "임시음식")
    private val dataListMed: MutableList<String> =
        mutableListOf("밥", "빵", "면", "국/찌개", "나물", "볶음", "구이", "찜", "튀김", "디저트", "음료", "기타ㅁ")
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
        recyclerViewSearchBig = findViewById(R.id.RecyclerViewCategoryBig)
        recyclerViewSearchMed = findViewById(R.id.RecyclerViewCategoryMed)
        recyclerViewSearchSmall = findViewById(R.id.RecyclerViewCategorySmall)

        UsefulRecycler.setupRecyclerView(recyclerViewSearchBig, CategoryAdapterBig, this, 1)
        UsefulRecycler.setupRecyclerView(recyclerViewSearchMed, CategoryAdapterMed, this, 2)
        UsefulRecycler.setupRecyclerView(recyclerViewSearchSmall, CategoryAdapterSmall, this, 1)
        
        // 테스트용 유저 설정 불러오기
        val userData = UserManager.getUser()!!
        val userName = findViewById<TextView>(R.id.placeholder_name)
        val userPoint = findViewById<TextView>(R.id.salt_placeholder)
        val userImage = findViewById<ImageView>(R.id.UserImageView)

        userName.text = userData.Name
        userPoint.text = userData.point.toString() + getString(R.string.title_salt)
        userImage.id = userData.image
        //

        categoryMenuBar.visibility = View.INVISIBLE

        contentScrollView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_MOVE -> {
                    checkScrollPosition()
                }
            }
            false
        }
        // 타이머 초기화 및 검색창 숨김 로직 설정
        hideSearchBarRunnable = Runnable {
            if (!searchBarHidden&&contentScrollView.scrollY>10) hideSearchView()
        }


        homeSearchBar.setOnQueryTextFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                showCategories()
            } else {
                hideCategories()
            }
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val currentFocusView = currentFocus
            if (currentFocusView != null) {
                val outRect = Rect()
                currentFocusView.getGlobalVisibleRect(outRect)
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    currentFocusView.clearFocus()
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(currentFocusView.windowToken, 0)
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }


    private fun checkScrollPosition() {
        if (contentScrollView.scrollY <= 0) {
            if (searchBarHidden) {
                showSearchView()
            }
        }
        resetHideSearchBarTimer()
    }

    private fun showCategories() {
        categoryMenuBar.visibility = View.VISIBLE
        ViewAnimator.moveYPos(categoryMenuBar, -600f, 0f, 300, DecelerateInterpolator(2f)) {
            categoryMenuBar.isClickable = true
        }.start()
        categoryBarHidden = false
    }

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
    private fun resetHideSearchBarTimer() {
        // 기존 타이머 취소
        hideSearchBarRunnable?.let { handler.removeCallbacks(it) }
        // 새로운 타이머 설정
        handler.postDelayed(hideSearchBarRunnable!!, hideSearchBarDelay)
    }

    private fun showSearchView() {
        homeSearchBarAppMenu.visibility = View.VISIBLE
        ViewAnimator.moveYPos(homeSearchBarAppMenu, -120f, 0f, 300, DecelerateInterpolator(2f)) {
            homeSearchBarAppMenu.isClickable = true
        }.start()
        ViewAnimator.moveYPos(contentScrollView, -120f, 0f, 300, DecelerateInterpolator(2f)).start()
        homeSearchBar.isFocusable = true
        homeSearchBar.isFocusableInTouchMode = true
        searchBarHidden = false
    }

    private fun hideSearchView() {
        ViewAnimator.moveYPos(
            homeSearchBarAppMenu,
            0f,
            -120f,
            300,
            AccelerateInterpolator(2f)
        ) {
            homeSearchBarAppMenu.visibility = View.INVISIBLE;homeSearchBarAppMenu.isClickable =
            false
        }.start()
        ViewAnimator.moveYPos(
            contentScrollView,
            0f,
            -120f,
            300,
            AccelerateInterpolator(2f)
        ).start()
        homeSearchBar.clearFocus()
        searchBarHidden = true
    }
}