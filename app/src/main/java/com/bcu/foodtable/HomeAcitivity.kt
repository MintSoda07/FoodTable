package com.bcu.foodtable

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.widget.ScrollView
import android.widget.SearchView
import android.widget.TextView
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.bcu.foodtable.databinding.ActivityHomeAcitivityBinding
import com.bcu.foodtable.useful.UserManager
import com.bcu.foodtable.useful.ViewAnimator


class HomeAcitivity : AppCompatActivity() {

    lateinit var contentScrollView: ScrollView
    lateinit var homeSearchBar: SearchView
    lateinit var homeSearchBarAppMenu: View
    private var searchBarHidden = false

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

        // 테스트용 유저 설정 불러오기
        val userData = UserManager.getUser()!!
        val userName = findViewById<TextView>(R.id.placeholder_name)
        val userPoint = findViewById<TextView>(R.id.salt_placeholder)

        userName.text = userData.Name
        userPoint.text = userData.point.toString() + getString(R.string.title_salt)
        //


        contentScrollView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_MOVE -> {
                    checkScrollPosition()
                }
            }
            false
        }

        homeSearchBar.onFocusChangeListener =View.OnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {

            } else {

            }
        }
    }

    private fun checkScrollPosition() {
        if (contentScrollView.scrollY <= 15) {
            if (searchBarHidden)
                showSearchView()
        } else {
            if (!searchBarHidden)
                hideSearchView()
        }
    }
    private fun showCategories(){
        
    }
    private fun hideCategories(){

    }

    private fun showSearchView() {
        homeSearchBarAppMenu.visibility = View.VISIBLE
        ViewAnimator.moveYPos(homeSearchBarAppMenu, -150f, 0f, 200, AccelerateInterpolator(1.2f)) {
            homeSearchBarAppMenu.isClickable = true
        }
            .start()
        homeSearchBar.isFocusable = true
        homeSearchBar.isFocusableInTouchMode = true
        searchBarHidden = false
    }

    private fun hideSearchView() {
        ViewAnimator.moveYPos(
            homeSearchBarAppMenu,
            0f,
            -150f,
            200,
            AccelerateInterpolator(1.2f)
        ) {
            homeSearchBarAppMenu.visibility = View.INVISIBLE;homeSearchBarAppMenu.isClickable =
            false
        }.start()
        homeSearchBar.clearFocus()
        searchBarHidden = true
    }
}