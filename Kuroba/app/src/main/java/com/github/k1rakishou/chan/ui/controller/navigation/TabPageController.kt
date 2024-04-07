package com.github.k1rakishou.chan.ui.controller.navigation

import android.content.Context
import com.github.k1rakishou.chan.features.toolbar.KurobaToolbarState
import com.github.k1rakishou.chan.ui.controller.base.Controller

abstract class TabPageController(
  context: Context,
) : Controller(context) {
  abstract fun updateToolbarState(): KurobaToolbarState
  abstract fun onTabFocused()
  abstract fun canSwitchTabs(): Boolean
}