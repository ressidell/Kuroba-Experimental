package com.github.k1rakishou.chan.controller.transition

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

class FadeInTransition : ControllerTransition() {

  override fun perform() {
    animatorSet.end()

    val toAlpha: Animator = ObjectAnimator.ofFloat(to!!.view, View.ALPHA, 0f, 1f)
    toAlpha.duration = 200
    toAlpha.interpolator = AccelerateDecelerateInterpolator()

    toAlpha.addListener(object : AnimatorListenerAdapter() {
      override fun onAnimationCancel(animation: Animator) {
        onCompleted()
      }

      override fun onAnimationEnd(animation: Animator) {
        onCompleted()
      }
    })

    animatorSet.playTogether(toAlpha)
    animatorSet.start()
  }
}