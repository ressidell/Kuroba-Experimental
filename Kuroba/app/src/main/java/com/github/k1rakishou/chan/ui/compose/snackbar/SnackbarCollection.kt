package com.github.k1rakishou.chan.ui.compose.snackbar

class SnackbarCollection {
  private val _addedSnackbars = mutableSetOf<SnackbarId>()

  operator fun plusAssign(snackbarId: SnackbarId) {
    _addedSnackbars += snackbarId
  }

  fun dismissAll(snackbarManager: SnackbarManager) {
    _addedSnackbars.forEach { snackbarId -> snackbarManager.dismissSnackbar(snackbarId) }
    _addedSnackbars.clear()
  }

}