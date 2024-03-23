package com.github.k1rakishou.chan.controller.transition

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.AccelerateInterpolator

class PopControllerTransition : ControllerTransition() {

  override fun perform() {
    animatorSet.end()

    val fromY: Animator = ObjectAnimator.ofFloat(from!!.view, View.TRANSLATION_Y, 0f, from!!.view.height * 0.05f)
    fromY.interpolator = AccelerateInterpolator(2.5f)
    fromY.duration = 250

    fromY.addListener(object : AnimatorListenerAdapter() {
      override fun onAnimationCancel(animation: Animator) {
        onCompleted()
      }

      override fun onAnimationEnd(animation: Animator) {
        onCompleted()
      }
    })

    val fromAlpha: Animator = ObjectAnimator.ofFloat(from!!.view, View.ALPHA, from!!.view.alpha, 0f)
    fromAlpha.interpolator = AccelerateInterpolator(2f)
    fromAlpha.startDelay = 100
    fromAlpha.duration = 150

    animatorSet.playTogether(fromY, fromAlpha)
    animatorSet.start()
  }

}