package com.github.k1rakishou.chan.ui.globalstate.snackbar

import androidx.compose.runtime.State
import com.github.k1rakishou.chan.ui.compose.snackbar.SnackbarControllerType
import com.github.k1rakishou.core_logger.Logger

interface ISnackbarGlobalState {
  interface Readable {
    fun snackbarVisibilityState(snackbarControllerType: SnackbarControllerType): State<Boolean>
  }

  interface Writeable {
    fun updateSnackbarVisibility(snackbarControllerType: SnackbarControllerType, visible: Boolean)
  }
}

class SnackbarGlobalState : ISnackbarGlobalState.Readable, ISnackbarGlobalState.Writeable {
  private val _states = mutableMapOf<SnackbarControllerType, IndividualSnackbarGlobalState>()

  override fun snackbarVisibilityState(snackbarControllerType: SnackbarControllerType): State<Boolean> {
    return getOrCreateState(snackbarControllerType).visibleState
  }

  override fun updateSnackbarVisibility(snackbarControllerType: SnackbarControllerType, visible: Boolean) {
    Logger.verbose(snackbarControllerType.tag()) { "updateSnackbarVisibility() visible: ${visible}" }
    getOrCreateState(snackbarControllerType).updateSnackbarVisibility(visible)
  }

  private fun getOrCreateState(snackbarControllerType: SnackbarControllerType): IndividualSnackbarGlobalState {
   return _states.getOrPut(snackbarControllerType, { IndividualSnackbarGlobalState() })
  }

  private fun SnackbarControllerType.tag(): String {
    return "SnackbarGlobalState_${this.name}"
  }

}