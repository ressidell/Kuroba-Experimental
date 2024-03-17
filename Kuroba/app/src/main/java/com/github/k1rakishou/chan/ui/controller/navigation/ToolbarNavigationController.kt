package com.github.k1rakishou.chan.ui.controller.navigation

import android.content.Context
import com.github.k1rakishou.chan.controller.Controller
import com.github.k1rakishou.chan.controller.transition.ControllerTransition

abstract class ToolbarNavigationController(context: Context) : NavigationController(context) {

  override fun transition(
    from: Controller?,
    to: Controller?,
    pushing: Boolean,
    controllerTransition: ControllerTransition?
  ) {
    if (from == null && to == null) {
      return
    }

    super.transition(from, to, pushing, controllerTransition)

    if (to != null) {
      toolbarState.showToolbar()
    }
  }

  override fun beginSwipeTransition(from: Controller?, to: Controller?): Boolean {
    if (from == null && to == null) {
      return false
    }

    if (!super.beginSwipeTransition(from, to)) {
      return false
    }

    toolbarState.showToolbar()

    if (to != null) {
      toolbarState.onTransitionStart(to.toolbarState)
    }

    return true
  }

  override fun swipeTransitionProgress(progress: Float) {
    super.swipeTransitionProgress(progress)

    toolbarState.onTransitionProgress(progress)
  }

  override fun endSwipeTransition(from: Controller?, to: Controller?, finish: Boolean) {
    if (from == null && to == null) {
      return
    }

    super.endSwipeTransition(from, to, finish)

    toolbarState.showToolbar()

    if (to != null) {
      toolbarState.onTransitionFinished(finish)
    }
  }

  override fun onBack(): Boolean {
    return toolbarState.onBack() || super.onBack()
  }

}
