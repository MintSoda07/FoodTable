package com.bcu.foodtable.useful

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle

// 액티비티 간의 화면 전환 시 적용할 수 있는 커스텀 object.
// startStatic을 사용하면 부드럽게 화면을 전환할 수 있다.
object ActivityTransition {

    fun startAnim(
        currentActivity: Activity,
        targetActivity: Class<out Activity>,
        enterAnim: Int,
        exitAnim: Int,
        extras: Bundle? = null
    ) {
        val intent = Intent(currentActivity, targetActivity)
        extras?.let {
            intent.putExtras(it)
        }
        currentActivity.startActivity(intent)
        currentActivity.overridePendingTransition(enterAnim, exitAnim)
    }

    fun startStatic(
        currentActivity: Activity,
        targetActivity: Class<out Activity>,
    ) {
        val intent = Intent(currentActivity, targetActivity)
        currentActivity.startActivity(intent)
    }
    fun startStaticInFragment(
        context: Context?,
        targetActivity: Class<out Activity>,
    ) {
        val intent = Intent(context, targetActivity)
        context?.startActivity(intent)
    }

    fun finish(
        currentActivity: Activity,
        enterAnim: Int,
        exitAnim: Int
    ) {
        currentActivity.finish()
        currentActivity.overridePendingTransition(enterAnim, exitAnim)
    }
}