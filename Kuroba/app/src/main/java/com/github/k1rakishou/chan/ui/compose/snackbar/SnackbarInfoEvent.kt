package com.github.k1rakishou.chan.ui.compose.snackbar

sealed class SnackbarInfoEvent {
  data class Push(val snackbarInfo: SnackbarInfo) : SnackbarInfoEvent()
  data class Pop(val id: SnackbarId) : SnackbarInfoEvent()
  data object PopAll : SnackbarInfoEvent()
}