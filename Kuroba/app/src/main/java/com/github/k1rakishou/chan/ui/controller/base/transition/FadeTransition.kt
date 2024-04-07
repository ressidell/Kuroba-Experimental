package com.github.k1rakishou.chan.ui.controller.base.transition

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

class FadeTransition(
  transitionMode: TransitionMode,
) : ControllerTransition(transitionMode) {

  override fun perform() {
    animatorSet.end()

    val toAlpha = when (transitionMode) {
      TransitionMode.In -> {
        ObjectAnimator.ofFloat(to!!.view, View.ALPHA, 0f, 1f)
      }
      TransitionMode.Out -> {
        ObjectAnimator.ofFloat(from!!.view, View.ALPHA, from!!.view.alpha, 0f)
      }
    }

    toAlpha.duration = 200
    toAlpha.interpolator = AccelerateDecelerateInterpolator()

    val progress = when (transitionMode) {
      TransitionMode.In -> ObjectAnimator.ofFloat(0f, 1f)
      TransitionMode.Out -> ObjectAnimator.ofFloat(from!!.view.alpha, 0f)
    }
    progress.interpolator = AccelerateDecelerateInterpolator()
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

    animatorSet.playTogether(toAlpha, progress)
    animatorSet.start()
  }
}