package com.bcu.foodtable.JetpackCompose.Mypage.Setting

import android.content.Context
import android.content.Context.MODE_PRIVATE

object HealthPrefs {
    private const val FILE = "health_prefs"
    private const val KEY = "health_enabled"

    fun isEnabled(context: Context) =
        context.getSharedPreferences(FILE, MODE_PRIVATE).getBoolean(KEY, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(FILE, MODE_PRIVATE)
            .edit().putBoolean(KEY, enabled).apply()
    }
}
