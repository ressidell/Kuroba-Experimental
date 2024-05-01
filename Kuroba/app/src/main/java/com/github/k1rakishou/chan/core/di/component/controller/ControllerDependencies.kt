package com.github.k1rakishou.chan.core.di.component.controller

import com.github.k1rakishou.chan.core.di.module.controller.ControllerScopedViewModelFactory

interface ControllerDependencies {
  val controllerScopedViewModelFactory: ControllerScopedViewModelFactory
}