package com.bcu.foodtable.useful

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator


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

    fun finish(
        currentActivity: Activity,
        enterAnim: Int,
        exitAnim: Int
    ) {
        currentActivity.finish()
        currentActivity.overridePendingTransition(enterAnim, exitAnim)
    }
}