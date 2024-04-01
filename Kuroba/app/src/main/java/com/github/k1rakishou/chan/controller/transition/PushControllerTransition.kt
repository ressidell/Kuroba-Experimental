package com.github.k1rakishou.chan.controller.transition

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.view.doOnPreDraw

class PushControllerTransition : ControllerTransition(TransitionMode.In) {

  override fun perform() {
    to!!.view.doOnPreDraw {
      animatorSet.end()

      val toAlpha: Animator = ObjectAnimator.ofFloat(to!!.view, View.ALPHA, 0f, 1f)
      toAlpha.duration = 200
      toAlpha.interpolator = DecelerateInterpolator(2f)

      val toY: Animator = ObjectAnimator.ofFloat(to!!.view, View.TRANSLATION_Y, to!!.view.height * 0.08f, 0f)
      toY.duration = 350
      toY.interpolator = DecelerateInterpolator(2.5f)

      val progress = ObjectAnimator.ofFloat(0f, 1f)
      progress.interpolator = DecelerateInterpolator(2.5f)
      progress.duration = 350
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

      animatorSet.playTogether(toAlpha, toY, progress)
      animatorSet.start()
    }
  }

}