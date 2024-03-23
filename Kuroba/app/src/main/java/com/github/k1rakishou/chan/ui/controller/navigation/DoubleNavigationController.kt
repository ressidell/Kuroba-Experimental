package com.github.k1rakishou.chan.ui.controller.navigation

import com.github.k1rakishou.chan.controller.Controller
import com.github.k1rakishou.chan.core.navigation.ControllerWithNavigation
import com.github.k1rakishou.chan.core.navigation.HasNavigation

interface DoubleNavigationController : ControllerWithNavigation, HasNavigation {
  fun setLeftController(leftController: Controller?, animated: Boolean)
  fun setRightController(rightController: Controller?, animated: Boolean)
  fun getLeftController(): Controller?
  fun getRightController(): Controller?

  fun switchToController(leftController: Boolean, animated: Boolean)
  fun switchToController(leftController: Boolean)
  fun openControllerWrappedIntoBottomNavAwareController(controller: Controller?)
}