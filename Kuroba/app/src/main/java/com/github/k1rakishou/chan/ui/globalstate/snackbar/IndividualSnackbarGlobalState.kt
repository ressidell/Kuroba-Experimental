package com.github.k1rakishou.chan.ui.globalstate.snackbar

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf

interface IIndividualSnackbarGlobalState {
  interface Readable {
    val visibleState: State<Boolean>
  }

  interface Writeable {
    fun updateSnackbarVisibility(visible: Boolean)
  }
}

internal class IndividualSnackbarGlobalState : IIndividualSnackbarGlobalState.Readable, IIndividualSnackbarGlobalState.Writeable {
  private val _visibleState = mutableStateOf(false)
  override val visibleState: State<Boolean>
    get() = _visibleState

  override fun updateSnackbarVisibility(visible: Boolean) {
    _visibleState.value = visible
  }

}