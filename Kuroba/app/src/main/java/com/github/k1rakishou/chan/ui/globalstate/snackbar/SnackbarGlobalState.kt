package com.github.k1rakishou.chan.ui.globalstate.snackbar

import androidx.compose.runtime.State
import com.github.k1rakishou.chan.ui.widget.SnackbarClass
import com.github.k1rakishou.core_logger.Logger

interface ISnackbarGlobalState {
  interface Readable {
    fun snackbarVisibilityState(snackbarClass: SnackbarClass): State<Boolean>
  }

  interface Writeable {
    fun updateSnackbarVisibility(snackbarClass: SnackbarClass, visible: Boolean)
  }
}

class SnackbarGlobalState : ISnackbarGlobalState.Readable, ISnackbarGlobalState.Writeable {
  private val _states = mutableMapOf<SnackbarClass, IndividualSnackbarGlobalState>()

  override fun snackbarVisibilityState(snackbarClass: SnackbarClass): State<Boolean> {
    return getOrCreateState(snackbarClass).visibleState
  }

  override fun updateSnackbarVisibility(snackbarClass: SnackbarClass, visible: Boolean) {
    Logger.verbose(snackbarClass.tag()) { "updateSnackbarVisibility() visible: ${visible}" }
    getOrCreateState(snackbarClass).updateSnackbarVisibility(visible)
  }

  private fun getOrCreateState(snackbarClass: SnackbarClass): IndividualSnackbarGlobalState {
   return _states.getOrPut(snackbarClass, { IndividualSnackbarGlobalState() })
  }

  private fun SnackbarClass.tag(): String {
    return "SnackbarGlobalState_${this.name}"
  }

}