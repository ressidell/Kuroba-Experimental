package com.github.k1rakishou.chan.controller.transition

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.AccelerateInterpolator

class PopControllerTransition : ControllerTransition(TransitionMode.Out) {

  override fun perform() {
    animatorSet.end()

    val fromY = ObjectAnimator.ofFloat(from!!.view, View.TRANSLATION_Y, 0f, from!!.view.height * 0.05f)
    fromY.interpolator = AccelerateInterpolator(2.5f)
    fromY.duration = 250

    val fromAlpha = ObjectAnimator.ofFloat(from!!.view, View.ALPHA, from!!.view.alpha, 0f)
    fromAlpha.interpolator = AccelerateInterpolator(2f)
    fromAlpha.startDelay = 100
    fromAlpha.duration = 150

    val progress = ObjectAnimator.ofFloat(from!!.view.alpha, 0f)
    progress.interpolator = AccelerateInterpolator(2f)
    progress.duration = 250
    progress.addUpdateListener { animator ->
      onAnimationProgress(animator.animatedValue as Float)
    }

    progress.addListener(
      object : AnimatorListenerAdapter() {
        override fun onAnimationStart(animation: Animator, isReverse: Boolean) {
          onAnimationStarted()
        }

        override fun onAnimationCancel(animation: Animator) {
          onAnimationCompleted()
        }

        override fun onAnimationEnd(animation: Animator) {
          onAnimationCompleted()
        }
      }
    )

    animatorSet.playTogether(fromY, fromAlpha, progress)
    animatorSet.start()
  }

}