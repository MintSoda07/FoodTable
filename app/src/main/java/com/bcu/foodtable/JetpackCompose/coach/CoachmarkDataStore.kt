package com.bcu.foodtable.JetpackCompose.coach

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

/**
 * DataStore 이름
 */
private const val DS_NAME = "coachmark_prefs"

/**
 * Context.coachDataStore 확장
 */
val Context.coachDataStore by preferencesDataStore(DS_NAME)

/**
 * 코치마크를 보여줄 화면들 (투어 시퀀스의 각 스크린)
 * - HOME -> SUBSCRIBE -> SOCIAL -> STORAGE -> PROFILE
 */
enum class CoachScreen { HOME, SUBSCRIBE, SOCIAL, STORAGE, PROFILE }

/**
 * 코치마크 노출 여부 저장소 인터페이스
 * - 오버레이에서 isSeen/setSeen을 호출
 * - 전체 초기화(resetAll)는 디버그/설정에서 재시작 용도로 사용
 */
// 인터페이스에 투어 완료 플래그 API
interface CoachmarkStore {
    suspend fun isSeen(screen: CoachScreen): Boolean
    suspend fun setSeen(screen: CoachScreen, seen: Boolean)
    suspend fun resetAll()


    suspend fun isTourDone(): Boolean
    suspend fun setTourDone(done: Boolean)
}

/**
 * DataStore 구현체
 * - 각 스크린별로 seen 플래그를 저장/조회
 */
class CoachmarkStoreDataStore(private val context: Context) : CoachmarkStore {

    private val KEY_HOME = booleanPreferencesKey("coach_home_seen")
    private val KEY_SUB  = booleanPreferencesKey("coach_sub_seen")
    private val KEY_SOC  = booleanPreferencesKey("coach_soc_seen")
    private val KEY_STO  = booleanPreferencesKey("coach_sto_seen")
    private val KEY_PRO  = booleanPreferencesKey("coach_pro_seen")
    private val KEY_TOUR_DONE = booleanPreferencesKey("coach_tour_done")
    private fun key(screen: CoachScreen) = when (screen) {
        CoachScreen.HOME      -> KEY_HOME
        CoachScreen.SUBSCRIBE -> KEY_SUB
        CoachScreen.SOCIAL    -> KEY_SOC
        CoachScreen.STORAGE   -> KEY_STO
        CoachScreen.PROFILE   -> KEY_PRO
    }

    override suspend fun isSeen(screen: CoachScreen): Boolean {
        val prefs = context.coachDataStore.data.first()
        return prefs[key(screen)] ?: false
    }

    override suspend fun setSeen(screen: CoachScreen, seen: Boolean) {
        context.coachDataStore.edit { it[key(screen)] = seen }
    }

    override suspend fun resetAll() {
        context.coachDataStore.edit {
            it[KEY_HOME] = false
            it[KEY_SUB]  = false
            it[KEY_SOC]  = false
            it[KEY_STO]  = false
            it[KEY_PRO]  = false
        }
    }
    override suspend fun isTourDone(): Boolean {
        val prefs = context.coachDataStore.data.first()
        return prefs[KEY_TOUR_DONE] ?: false
    }

    override suspend fun setTourDone(done: Boolean) {
        context.coachDataStore.edit { it[KEY_TOUR_DONE] = done }
    }
}
