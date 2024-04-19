package com.github.k1rakishou.chan.ui.compose.snackbar

sealed class SnackbarType {
  val isToast: Boolean
    get() = this is Toast || this is ErrorToast

  data object Default : SnackbarType()
  data object Toast : SnackbarType()
  data object ErrorToast : SnackbarType()
}

enum class SnackbarControllerType {
  Main,
  Catalog,
  Thread
}