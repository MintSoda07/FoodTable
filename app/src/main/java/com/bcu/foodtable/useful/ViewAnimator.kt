package com.bcu.foodtable.useful

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.LinearInterpolator


object ViewAnimator {
    fun moveXPos(
        view: View,
        initXPos: Float,
        lastXPos: Float,
        durationOfAnim: Long,
        interpolator: TimeInterpolator? = LinearInterpolator()
    ): ValueAnimator {
        val title_xPosAnimator: ValueAnimator = ObjectAnimator
            .ofFloat(view, "translationX", initXPos, lastXPos).apply {
                duration = durationOfAnim
                interpolator?.let { this.interpolator = it }
            }
        return title_xPosAnimator
    }

    fun moveYPos(
        view: View,
        initYPos: Float,
        lastYPos: Float,
        durationOfAnim: Long,
        interpolator: TimeInterpolator? = LinearInterpolator()
    ): ValueAnimator {
        val title_xPosAnimator: ValueAnimator = ObjectAnimator
            .ofFloat(view, "translationY", initYPos, lastYPos).apply {
                duration = durationOfAnim
                interpolator?.let { this.interpolator = it }
            }
        return title_xPosAnimator
    }

    fun moveZPos(
        view: View,
        initZPos: Float,
        lastZPos: Float,
        durationOfAnim: Long,
        interpolator: TimeInterpolator? = LinearInterpolator()
    ): ValueAnimator {
        val title_xPosAnimator: ValueAnimator = ObjectAnimator
            .ofFloat(view, "translationZ", initZPos, lastZPos).apply {
                duration = durationOfAnim
                interpolator?.let { this.interpolator = it }
            }
        return title_xPosAnimator
    }
    fun Rotation(
        view: View,
        initRotation: Float,
        lastRotation: Float,
        durationOfAnim: Long,
        interpolator: TimeInterpolator? = LinearInterpolator()
    ): ValueAnimator {
        val rotationAnimator: ValueAnimator = ObjectAnimator
            .ofFloat(view, "rotation", initRotation, lastRotation).apply {
                duration = durationOfAnim
                interpolator?.let { this.interpolator = it }
            }
        return rotationAnimator
    }
    fun changeScale(
        view: View,
        initScaleX: Float,
        lastScaleX: Float,
        initScaleY: Float,
        lastScaleY: Float,
        durationOfAnim: Long,
        interpolator: TimeInterpolator? = LinearInterpolator()
    ): AnimatorSet {
        val scaleXAnimator = ObjectAnimator.ofFloat(view, "scaleX", initScaleX, lastScaleX).apply {
            duration = durationOfAnim
            interpolator?.let { this.interpolator = it }
        }
        val scaleYAnimator = ObjectAnimator.ofFloat(view, "scaleY", initScaleY, lastScaleY).apply {
            duration = durationOfAnim
            interpolator?.let { this.interpolator = it }
        }

        return AnimatorSet().apply {
            playTogether(scaleXAnimator, scaleYAnimator)
        }
    }

    fun alphaChange(
        view: View,
        durationOfAnim: Long,
        to_visible: Boolean,
        interpolator: TimeInterpolator? = LinearInterpolator()
    ): ObjectAnimator {
        val (startAlpha, endAlpha) = if (to_visible) 0f to 1f else 1f to 0f
        view.alpha = startAlpha
        return ObjectAnimator.ofFloat(view, "alpha", startAlpha, endAlpha).apply {
            duration = durationOfAnim
            interpolator?.let { this.interpolator = it }
        }
    }

    fun floatAnimation(
        view: View,
        durationOfAnim: Long,
        interpolator: TimeInterpolator? = LinearInterpolator()
    ): AnimatorSet {
        val alphaAnimator = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        val translationZAnimator = ObjectAnimator.ofFloat(view, "translationZ", 0f, 50f)
        val animatorSet = AnimatorSet().apply {
            playTogether(alphaAnimator, translationZAnimator)
            duration = durationOfAnim
            interpolator?.let { this.interpolator = it }
        }
        return animatorSet
    }

    fun alphaChangeDetail(
        view: View,
        startAlpha: Float,
        endAlpha: Float,
        durationOfAnim: Long,
        interpolator: TimeInterpolator? = LinearInterpolator()
    ): ObjectAnimator {
        val alphaAnimator = ObjectAnimator.ofFloat(view, "alpha", startAlpha, endAlpha).apply {
            duration = durationOfAnim
            interpolator?.let { this.interpolator = it }
        }
        return alphaAnimator
    }

    fun cancelAnimation(animator: Animator) {
        if (animator.isRunning || animator.isStarted) {
            animator.cancel()
        }
    }

    fun resetAnimation(view: View) {
        view.translationX = 0f
        view.translationY = 0f
        view.translationZ = 0f
        view.alpha = 1f
        view.rotation = 0f
        view.scaleX = 1f
        view.scaleY = 1f
    }

}