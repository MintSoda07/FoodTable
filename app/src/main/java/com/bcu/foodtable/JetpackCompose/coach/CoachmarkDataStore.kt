package com.bcu.foodtable.JetpackCompose.coach

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private const val DS_NAME = "coachmark_prefs"
val Context.coachDataStore by preferencesDataStore(DS_NAME)

enum class CoachScreen { HOME, SUBSCRIBE, SOCIAL, STORAGE, PROFILE }

interface CoachmarkStore {
    suspend fun isSeen(screen: CoachScreen): Boolean
    suspend fun setSeen(screen: CoachScreen, seen: Boolean)
    suspend fun resetAll()
}

class CoachmarkStoreDataStore(private val context: Context) : CoachmarkStore {
    private val KEY_HOME = booleanPreferencesKey("coach_home_seen")
    private val KEY_SUB  = booleanPreferencesKey("coach_sub_seen")
    private val KEY_SOC  = booleanPreferencesKey("coach_soc_seen")
    private val KEY_STO  = booleanPreferencesKey("coach_sto_seen")
    private val KEY_PRO  = booleanPreferencesKey("coach_pro_seen")

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
}
