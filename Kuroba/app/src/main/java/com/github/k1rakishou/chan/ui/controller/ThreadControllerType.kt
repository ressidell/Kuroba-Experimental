package com.github.k1rakishou.chan.ui.controller

import com.github.k1rakishou.chan.ui.compose.snackbar.SnackbarScope

enum class ThreadControllerType {
  Catalog,
  Thread;

  fun asSnackbarScope(): SnackbarScope {
    return when (this) {
      Catalog -> SnackbarScope.Catalog
      Thread -> SnackbarScope.Thread
    }
  }
}