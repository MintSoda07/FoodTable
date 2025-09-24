//
//package com.bcu.foodtable.JetpackCompose.coach
//
//import android.content.Context
//import androidx.datastore.preferences.core.Preferences
//import androidx.datastore.preferences.core.booleanPreferencesKey
//import androidx.datastore.preferences.core.edit
//import androidx.datastore.preferences.preferencesDataStore
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.first
//import kotlinx.coroutines.flow.map
//
//private val Context.dataStore by preferencesDataStore(name = "coach_prefs")
//
//class TourPrefs(private val context: Context) {
//    companion object {
//        private val APP_TOUR_DONE = booleanPreferencesKey("app_tour_done")
//    }
//
//    /** Flow 로 관찰(필요 시) */
//    val doneFlow: Flow<Boolean> = context.dataStore.data.map { it[APP_TOUR_DONE] ?: false }
//
//    /** suspend 로 현재 값 1회 읽기 */
//    suspend fun isDone(): Boolean = doneFlow.first()
//
//    /** 완료/초기화 세팅 */
//    suspend fun setDone(done: Boolean) {
//        context.dataStore.edit { prefs: Preferences ->
//            prefs[APP_TOUR_DONE] = done
//        }
//    }
//}
