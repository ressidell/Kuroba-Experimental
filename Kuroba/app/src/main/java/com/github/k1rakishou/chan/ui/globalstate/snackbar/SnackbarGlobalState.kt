package com.github.k1rakishou.chan.ui.globalstate.snackbar

import androidx.compose.runtime.State
import com.github.k1rakishou.chan.ui.compose.snackbar.SnackbarScope
import com.github.k1rakishou.core_logger.Logger

interface ISnackbarGlobalState {
  interface Readable {
    fun snackbarVisibilityState(snackbarScope: SnackbarScope): State<Boolean>
  }

  interface Writeable {
    fun updateSnackbarVisibility(snackbarScope: SnackbarScope, visible: Boolean)
  }
}

class SnackbarGlobalState : ISnackbarGlobalState.Readable, ISnackbarGlobalState.Writeable {
  private val _states = mutableMapOf<SnackbarScope, IndividualSnackbarGlobalState>()

  override fun snackbarVisibilityState(snackbarScope: SnackbarScope): State<Boolean> {
    return getOrCreateState(snackbarScope).visibleState
  }

  override fun updateSnackbarVisibility(snackbarScope: SnackbarScope, visible: Boolean) {
    Logger.verbose(snackbarScope.tag()) { "updateSnackbarVisibility() visible: ${visible}" }
    getOrCreateState(snackbarScope).updateSnackbarVisibility(visible)
  }

  private fun getOrCreateState(snackbarScope: SnackbarScope): IndividualSnackbarGlobalState {
   return _states.getOrPut(snackbarScope, { IndividualSnackbarGlobalState() })
  }

  private fun SnackbarScope.tag(): String {
    return "SnackbarGlobalState_${this.tag}"
  }

}