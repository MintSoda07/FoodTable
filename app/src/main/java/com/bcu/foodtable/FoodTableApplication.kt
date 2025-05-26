package com.bcu.foodtable

import android.app.Application
import com.bcu.foodtable.di.AppContainer

class FoodTableApplication : Application() {

    // 전역에서 사용할 수 있는 AppContainer
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer()
    }
}
