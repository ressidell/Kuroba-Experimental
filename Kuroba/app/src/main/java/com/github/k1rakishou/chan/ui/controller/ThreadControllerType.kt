package com.github.k1rakishou.chan.ui.controller

import com.github.k1rakishou.chan.ui.compose.snackbar.SnackbarControllerType

enum class ThreadControllerType {
  Catalog,
  Thread;

  fun asSnackbarControllerType(): SnackbarControllerType {
    return when (this) {
      Catalog -> SnackbarControllerType.Catalog
      Thread -> SnackbarControllerType.Thread
    }
  }
}