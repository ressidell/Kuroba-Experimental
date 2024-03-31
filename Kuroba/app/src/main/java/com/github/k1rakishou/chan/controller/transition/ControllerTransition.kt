package com.github.k1rakishou.chan.controller.transition

import android.animation.AnimatorSet
import com.github.k1rakishou.chan.controller.Controller
import com.github.k1rakishou.chan.ui.controller.navigation.NavigationController

abstract class ControllerTransition(
  protected val transitionMode: TransitionMode
)  {
  protected val animatorSet = AnimatorSet()

  private var listener: TransitionFinishListener? = null
  private var transitionStarted = false

  @JvmField
  var navigationController: NavigationController? = null
  @JvmField
  var from: Controller? = null
  @JvmField
  var to: Controller? = null

  fun onStarted() {
    val navController = navigationController
      ?: return

    val controllerToolbarState = when (transitionMode) {
      TransitionMode.In -> to?.toolbarState
      TransitionMode.Out -> from?.toolbarState
    }

    if (controllerToolbarState == null) {
      return
    }

    transitionStarted = true
    navController.toolbarState.onTransitionProgressStart(controllerToolbarState, transitionMode)
  }

  fun onProgress(progress: Float) {
    val navController = navigationController

    if (transitionStarted && navController != null) {
      navController.toolbarState.onTransitionProgress(progress)
    }
  }

  fun onCompleted() {
    val navController = navigationController

    val controllerToolbarState = when (transitionMode) {
      TransitionMode.In -> to?.toolbarState
      TransitionMode.Out -> from?.toolbarState
    }

    if (transitionStarted && navController != null && controllerToolbarState != null) {
      navController.toolbarState.onTransitionProgressFinished()
      navController.containerToolbarState = controllerToolbarState
    }

    listener?.onControllerTransitionCompleted(this)
    transitionStarted = false
  }

  fun onTransitionFinished(transitionFinishListener: TransitionFinishListener?) {
    listener = transitionFinishListener
  }

  fun interface TransitionFinishListener {
    fun onControllerTransitionCompleted(transition: ControllerTransition?)
  }

  abstract fun perform()

  override fun toString(): String {
    return "ControllerTransition(transition: ${this::class.java.simpleName}, transitionMode: $transitionMode)"
  }

}