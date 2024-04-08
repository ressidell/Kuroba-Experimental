package com.github.k1rakishou.chan.ui.controller.navigation

import com.github.k1rakishou.chan.core.navigation.ControllerWithNavigation
import com.github.k1rakishou.chan.core.navigation.HasNavigation
import com.github.k1rakishou.chan.features.toolbar.KurobaToolbarState
import com.github.k1rakishou.chan.ui.controller.base.Controller

interface DoubleNavigationController : ControllerWithNavigation, HasNavigation {
  val navigationType: NavigationType
  val leftControllerToolbarState: KurobaToolbarState?
  val rightControllerToolbarState: KurobaToolbarState?

  fun updateLeftController(leftController: Controller?, animated: Boolean)
  fun updateRightController(rightController: Controller?, animated: Boolean)
  fun leftController(): Controller?
  fun rightController(): Controller?

  fun switchToController(leftController: Boolean, animated: Boolean)
  fun switchToController(leftController: Boolean)

  enum class NavigationType {
    Split,
    Slide
  }
}

fun DoubleNavigationController?.doOnNavigation(
  splitNavigation: (() -> Unit)? = null,
  slideNavigation: (() -> Unit)? = null
) {
  val navigationController = this
  if (navigationController == null) {
    return
  }

  when (navigationController.navigationType) {
    DoubleNavigationController.NavigationType.Split -> splitNavigation?.invoke()
    DoubleNavigationController.NavigationType.Slide -> slideNavigation?.invoke()
  }
}