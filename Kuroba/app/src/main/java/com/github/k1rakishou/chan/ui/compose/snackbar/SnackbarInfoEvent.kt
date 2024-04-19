package com.github.k1rakishou.chan.ui.compose.snackbar

import com.github.k1rakishou.chan.ui.controller.base.ControllerKey

sealed class SnackbarInfoEvent {
  data class Push(val snackbarInfo: SnackbarInfo) : SnackbarInfoEvent()
  data class Pop(val id: SnackbarId) : SnackbarInfoEvent()
  data class RemoveAllForControllerKeys(val controllerKeys: Set<ControllerKey>) : SnackbarInfoEvent()
}