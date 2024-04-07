package com.github.k1rakishou.chan.ui.activity

import com.github.k1rakishou.chan.ui.controller.base.Controller

interface StartActivityCallbacks {
  fun openControllerWrappedIntoBottomNavAwareController(controller: Controller)
  fun setSettingsMenuItemSelected()
}