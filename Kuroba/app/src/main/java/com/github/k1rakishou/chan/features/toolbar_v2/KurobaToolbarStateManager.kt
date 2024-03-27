package com.github.k1rakishou.chan.features.toolbar_v2

import com.github.k1rakishou.chan.controller.ControllerKey
import com.github.k1rakishou.chan.ui.globalstate.GlobalUiStateHolder

class KurobaToolbarStateManager(
  private val globalUiStateHolder: GlobalUiStateHolder
) {
  private val kurobaToolbarStates = mutableMapOf<ControllerKey, KurobaToolbarState>()

  fun getOrCreate(controllerKey: ControllerKey): KurobaToolbarState {
    return kurobaToolbarStates.getOrPut(
      key = controllerKey,
      defaultValue = { KurobaToolbarState(controllerKey, globalUiStateHolder) }
    )
  }

}