package com.bcu.foodtable.JetpackCompose.coach

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import com.bcu.foodtable.ui.home.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/* ─────────────────────────────────────────────────────────────
 * Nav 디버그 유틸 (같은 파일에 둬서 바로 사용)
 * ───────────────────────────────────────────────────────────── */
private const val NAV_TAG = "NAV_TRACE"

private fun NavController.debugName(): String =
    "Nav@${hashCode().toString(16)} graph=${graph.id} start=${graph.startDestinationRoute}"

private fun NavDestination.routeOrId(): String =
    route ?: "id=${id}"

private fun logNavEvent(
    where: String,
    nav: NavController,
    dest: NavDestination? = nav.currentDestination,
    args: Bundle? = null,
    extra: String = ""
) {
    Log.i(
        NAV_TAG,
        "[$where] host=${nav.debugName()} dest=${dest?.routeOrId()} args=${args?.keySet()?.joinToString()} $extra"
    )
}

/* ─────────────────────────────────────────────────────────────
 * CoachTour 본체
 * ───────────────────────────────────────────────────────────── */
object CoachTour {
    /** 현재 어떤 화면의 코치마크를 보여줄지 */
    val currentScreen = MutableLiveData<CoachScreen?>(null)

    /** 투어 실행 중인지 */
    val running = MutableLiveData(false)

    /** 투어를 한 번만 시작 (이미 완료면 스킵) */
    suspend fun maybeStartOnce(nav: NavController, context: Context, store: CoachmarkStore) {
        logNavEvent("maybeStartOnce()", nav, extra = "tourDone=${store.isTourDone()}")
        if (store.isTourDone()) return
        start(nav, context, store)
    }

    /**
     * 투어 시작:
     * - DataStore의 seen 플래그 초기화
     * - HOME로 이동
     */
    fun start(nav: NavController, context: Context, store: CoachmarkStore) {
        CoroutineScope(Dispatchers.IO).launch { store.resetAll() }
        running.value = true
        currentScreen.value = CoachScreen.HOME

        logNavEvent("start():beforeNavigate", nav, extra = "-> ${Screen.Home.route}")
        nav.navigate(Screen.Home.route) {
            popUpTo(nav.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
        logNavEvent("start():afterNavigate", nav)
    }

    /** 공통 이동 래퍼 (로그 + currentScreen 업데이트 + navigate) */
    private fun goTo(
        nav: NavController,
        label: String,
        route: String,
        nextScreen: CoachScreen
    ) {
        currentScreen.value = nextScreen
        logNavEvent("goTo($label):beforeNavigate", nav, extra = "-> $route / next=$nextScreen")
        nav.navigate(route) { launchSingleTop = true }
        logNavEvent("goTo($label):afterNavigate", nav)
    }

    /** 단계별 라우팅 */
    fun shoot(count: Int, nav: NavController) {
        when (count) {
            1 -> goTo(nav, "1", Screen.RecipeStorage.route, CoachScreen.STORAGE)
            2 -> goTo(nav, "2", Screen.Social.route,         CoachScreen.SOCIAL)
            3 -> goTo(nav, "3", Screen.Subscribe.route,      CoachScreen.SUBSCRIBE)
            4 -> goTo(nav, "4", Screen.MyPage.route,         CoachScreen.PROFILE)
        }
    }

    /** 해당 화면의 코치마크가 완료되면 → 다음 화면으로 */
    fun next(nav: NavController, context: Context, store: CoachmarkStore) {
        logNavEvent("next():enter", nav, extra = "currentScreen=${currentScreen.value}")

        when (currentScreen.value) {
            CoachScreen.HOME      -> shoot(1, nav)
            CoachScreen.STORAGE   -> shoot(2, nav)
            CoachScreen.SOCIAL    -> shoot(3, nav)
            CoachScreen.SUBSCRIBE -> shoot(4, nav)

            CoachScreen.PROFILE -> {
                // 투어 완료
                currentScreen.value = null
                running.value = false
                CoroutineScope(Dispatchers.IO).launch { store.setTourDone(true) }

                logNavEvent("next():complete", nav, extra = "-> ${Screen.Home.route}")
                nav.navigate(Screen.Home.route) {
                    popUpTo(nav.graph.startDestinationId) { inclusive = false }
                    launchSingleTop = true
                }
                Toast.makeText(context, "이제 시작해 볼까요?", Toast.LENGTH_SHORT).show()
                logNavEvent("next():afterCompleteNavigate", nav)
            }
            else -> Unit
        }
    }

    fun cancel(nav: NavController) {
        currentScreen.value = null
        running.value = false
        logNavEvent("cancel():beforeNavigate", nav, extra = "-> ${Screen.Home.route}")
        nav.navigate(Screen.Home.route) { launchSingleTop = true }
        logNavEvent("cancel():afterNavigate", nav)
    }
}
