package com.github.k1rakishou.chan.controller.transition

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.view.doOnPreDraw

class PushControllerTransition : ControllerTransition() {

  override fun perform() {
    to!!.view.doOnPreDraw {
      animatorSet.end()

      val toAlpha: Animator = ObjectAnimator.ofFloat(to!!.view, View.ALPHA, 0f, 1f)
      toAlpha.duration = 200
      toAlpha.interpolator = DecelerateInterpolator(2f)

      val toY: Animator = ObjectAnimator.ofFloat(to!!.view, View.TRANSLATION_Y, to!!.view.height * 0.08f, 0f)
      toY.duration = 350
      toY.interpolator = DecelerateInterpolator(2.5f)

      toY.addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationCancel(animation: Animator) {
          onCompleted()
        }

        override fun onAnimationEnd(animation: Animator) {
          onCompleted()
        }
      })

      animatorSet.playTogether(toAlpha, toY)
      animatorSet.start()
    }
  }

}