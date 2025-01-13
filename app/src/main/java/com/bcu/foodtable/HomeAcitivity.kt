package com.bcu.foodtable

import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.bcu.foodtable.databinding.ActivityHomeAcitivityBinding

class HomeAcitivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeAcitivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomeAcitivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar: Toolbar = binding.toolbar
        setSupportActionBar(toolbar)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_home_acitivity)

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_profile, R.id.navigation_recipe
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }
}