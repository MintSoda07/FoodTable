package com.bcu.foodtable

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class AppLifecycleObserver(
    private val onEnterForeground: () -> Unit,
    private val onEnterBackground: () -> Unit
) : DefaultLifecycleObserver {

    override fun onStart(owner: LifecycleOwner) {
        onEnterForeground()
    }

    override fun onStop(owner: LifecycleOwner) {
        onEnterBackground()
    }
}
