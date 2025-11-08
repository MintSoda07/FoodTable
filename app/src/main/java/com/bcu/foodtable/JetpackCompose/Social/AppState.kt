package com.bcu.foodtable

import androidx.lifecycle.ProcessLifecycleOwner

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppState {
    private val _currentChatUid = MutableStateFlow<String?>(null)
    val currentChatUid: StateFlow<String?> = _currentChatUid.asStateFlow()

    @Volatile private var foreground = false

    fun setCurrentChat(uid: String?) {
        _currentChatUid.value = uid
    }

    fun isAppInForeground(): Boolean = foreground

    fun init(app: android.app.Application) {
        // 프로세스 포그라운드/백그라운드 관찰
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            AppLifecycleObserver(
                onEnterForeground = { foreground = true },
                onEnterBackground = { foreground = false }
            )
        )
    }
}
