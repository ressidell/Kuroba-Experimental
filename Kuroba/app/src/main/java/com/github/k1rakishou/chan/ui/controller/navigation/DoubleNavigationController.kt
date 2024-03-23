package com.github.k1rakishou.chan.ui.controller.navigation

import com.github.k1rakishou.chan.controller.Controller
import com.github.k1rakishou.chan.core.navigation.ControllerWithNavigation
import com.github.k1rakishou.chan.core.navigation.HasNavigation

interface DoubleNavigationController : ControllerWithNavigation, HasNavigation {
  fun updateLeftController(leftController: Controller?, animated: Boolean)
  fun updateRightController(rightController: Controller?, animated: Boolean)
  fun leftController(): Controller?
  fun rightController(): Controller?

  fun switchToController(leftController: Boolean, animated: Boolean)
  fun switchToController(leftController: Boolean)
  fun openControllerWrappedIntoBottomNavAwareController(controller: Controller?)
}