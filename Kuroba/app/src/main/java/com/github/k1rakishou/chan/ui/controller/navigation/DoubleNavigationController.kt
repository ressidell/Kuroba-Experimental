package com.github.k1rakishou.chan.ui.controller.navigation

import com.github.k1rakishou.chan.core.navigation.ControllerWithNavigation
import com.github.k1rakishou.chan.core.navigation.HasNavigation
import com.github.k1rakishou.chan.features.toolbar.KurobaToolbarState
import com.github.k1rakishou.chan.ui.controller.base.Controller

interface DoubleNavigationController : ControllerWithNavigation, HasNavigation {
  val leftControllerToolbarState: KurobaToolbarState?
  val rightControllerToolbarState: KurobaToolbarState?

  fun updateLeftController(leftController: Controller?, animated: Boolean)
  fun updateRightController(rightController: Controller?, animated: Boolean)
  fun leftController(): Controller?
  fun rightController(): Controller?

  fun switchToLeftController(animated: Boolean = true)
  fun switchToRightController(animated: Boolean = true)
}