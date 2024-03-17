package com.github.k1rakishou.chan.features.toolbar_v2

import com.github.k1rakishou.chan.controller.ControllerKey

class KurobaToolbarStateManager {
  private val kurobaToolbarStates = mutableMapOf<ControllerKey, KurobaToolbarState>()

  fun enableControllerToolbar(controllerKey: ControllerKey) {
    kurobaToolbarStates[controllerKey]?.enable()
  }

  fun disableControllerToolbar(controllerKey: ControllerKey) {
    kurobaToolbarStates[controllerKey]?.disable()
  }

  fun getOrCreate(controllerKey: ControllerKey): KurobaToolbarState {
    return kurobaToolbarStates.getOrPut(
      key = controllerKey,
      defaultValue = { KurobaToolbarState() }
    )
  }

}