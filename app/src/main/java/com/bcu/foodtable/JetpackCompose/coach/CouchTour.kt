package com.bcu.foodtable.JetpackCompose.coach

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import com.bcu.foodtable.ui.home.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object CoachTour {
    /** 현재 어떤 화면의 코치마크를 보여줄지 */
    val currentScreen = MutableLiveData<CoachScreen?>(null)

    /** 투어 실행 중인지 */
    val running = MutableLiveData(false)

    suspend fun maybeStartOnce(nav: NavController, context: Context, store: CoachmarkStore) {
        if (store.isTourDone()) return  // 이미 끝났으면 시작 X
        start(nav, context, store)
    }

    /**
     * 투어 시작
     * - DataStore의 seen 플래그를 전부 초기화해서 모든 화면에서 코치마크가 보이도록
     * - 홈으로 이동
     */
    fun start(nav: NavController, context: Context, store: CoachmarkStore) {
        CoroutineScope(Dispatchers.IO).launch {
            store.resetAll()
        }
        running.value = true
        currentScreen.value = CoachScreen.HOME
        nav.navigate(Screen.Home.route) {
            popUpTo(nav.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    /** 해당 화면의 코치마크가 '완료'되면 호출 → 다음 화면으로 이동 */
    fun next(nav: NavController, context: Context, store: CoachmarkStore) {
        when (currentScreen.value) {
            CoachScreen.HOME -> {
                currentScreen.value = CoachScreen.SUBSCRIBE
                nav.navigate(Screen.Subscribe.route) { launchSingleTop = true }
            }
            CoachScreen.SUBSCRIBE -> {
                currentScreen.value = CoachScreen.SOCIAL
                nav.navigate(Screen.Social.route) { launchSingleTop = true }
            }
            CoachScreen.SOCIAL -> {
                currentScreen.value = CoachScreen.STORAGE
                nav.navigate(Screen.RecipeStorage.route) { launchSingleTop = true }
            }
            CoachScreen.STORAGE -> {
                currentScreen.value = CoachScreen.PROFILE
                nav.navigate(Screen.MyPage.route) { launchSingleTop = true }
            }
            CoachScreen.PROFILE -> {
                currentScreen.value = null
                running.value = false

                // ⬇ 마지막에서 ‘투어 완료’ 저장
                CoroutineScope(Dispatchers.IO).launch {
                    store.setTourDone(true)
                }

                nav.navigate(Screen.Home.route) {
                    popUpTo(nav.graph.startDestinationId) { inclusive = false }
                    launchSingleTop = true
                }
                Toast.makeText(context, "이제 시작해 볼까요?", Toast.LENGTH_SHORT).show()
            }
            else -> Unit
        }
    }

    fun cancel(nav: NavController) {
        currentScreen.value = null
        running.value = false
        nav.navigate(Screen.Home.route) { launchSingleTop = true }
    }
}