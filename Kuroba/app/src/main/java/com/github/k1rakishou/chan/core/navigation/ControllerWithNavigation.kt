package com.github.k1rakishou.chan.core.navigation

import com.github.k1rakishou.chan.ui.controller.base.Controller
import com.github.k1rakishou.chan.ui.controller.base.transition.ControllerTransition

interface ControllerWithNavigation {
  fun pushController(to: Controller): Boolean
  fun pushController(to: Controller, animated: Boolean): Boolean
  fun pushController(to: Controller, transition: ControllerTransition?): Boolean
  fun pushController(to: Controller, onFinished: () -> Unit): Boolean

  fun popController(): Boolean
  fun popController(animated: Boolean): Boolean
  fun popController(transition: ControllerTransition?): Boolean
  fun popController(onFinished: () -> Unit): Boolean
}