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
      requireNavController().toolbarState.showToolbar()
    }
  }

  override fun beginSwipeTransition(from: Controller?, to: Controller?): Boolean {
    if (from == null && to == null) {
      return false
    }

    if (!super.beginSwipeTransition(from, to)) {
      return false
    }

    requireNavController().toolbarState.showToolbar()

    if (to != null) {
      requireNavController().toolbarState.onTransitionStart(to.toolbarState)
    }

    return true
  }

  override fun swipeTransitionProgress(progress: Float) {
    super.swipeTransitionProgress(progress)

    requireNavController().toolbarState.onTransitionProgress(progress)
  }

  override fun endSwipeTransition(from: Controller?, to: Controller?, finish: Boolean) {
    if (from == null && to == null) {
      return
    }

    super.endSwipeTransition(from, to, finish)

    requireNavController().toolbarState.showToolbar()

    if (finish && to != null) {
      requireNavController().toolbarState.onTransitionFinished(to.toolbarState)
    } else if (!finish && from != null) {
      requireNavController().toolbarState.onTransitionFinished(from.toolbarState)
    }
  }

  override fun onBack(): Boolean {
    return requireNavController().toolbarState.onBack() || super.onBack()
  }

}
