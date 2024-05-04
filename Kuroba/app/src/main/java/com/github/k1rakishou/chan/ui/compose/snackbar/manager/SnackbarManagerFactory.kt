package com.github.k1rakishou.chan.ui.compose.snackbar.manager

import android.content.Context
import com.github.k1rakishou.chan.ui.compose.snackbar.SnackbarManager
import com.github.k1rakishou.chan.ui.compose.snackbar.SnackbarScope
import com.github.k1rakishou.chan.ui.globalstate.GlobalUiStateHolder

class SnackbarManagerFactory(
  private val appContext: Context,
  private val globalUiStateHolder: GlobalUiStateHolder
) {
  private val snackbarManagers = mutableMapOf<SnackbarScope, SnackbarManager>()

  fun snackbarManager(snackbarScope: SnackbarScope): SnackbarManager {
    return snackbarManagers.getOrPut(snackbarScope, { createSnackbarManager(snackbarScope) })
  }

  private fun createSnackbarManager(snackbarScope: SnackbarScope): SnackbarManager {
    return ScopedSnackbarManager(
      appContext = appContext,
      snackbarScope = snackbarScope,
      globalUiStateHolder = globalUiStateHolder
    )
  }

}