package com.github.k1rakishou.chan.ui.compose.snackbar.manager

import android.content.Context
import com.github.k1rakishou.chan.ui.compose.snackbar.SnackbarScope
import com.github.k1rakishou.chan.ui.globalstate.GlobalUiStateHolder

class CatalogSnackbarManager(
  appContext: Context,
  globalUiStateHolder: GlobalUiStateHolder
) : ScopedSnackbarManager(
  appContext = appContext,
  snackbarScope = SnackbarScope.Catalog,
  globalUiStateHolder = globalUiStateHolder
)