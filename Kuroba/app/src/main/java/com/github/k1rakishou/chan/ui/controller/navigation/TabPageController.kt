package com.github.k1rakishou.chan.ui.controller.navigation

import android.content.Context
import com.github.k1rakishou.chan.controller.Controller
import com.github.k1rakishou.chan.features.toolbar_v2.KurobaToolbarState

abstract class TabPageController(
  context: Context,
) : Controller(context) {
  abstract fun updateToolbarState(): KurobaToolbarState
  abstract fun onTabFocused()
  abstract fun canSwitchTabs(): Boolean
}