package com.github.k1rakishou.chan.features.drawer

import android.view.MotionEvent
import com.github.k1rakishou.chan.ui.controller.base.ControllerKey
import com.github.k1rakishou.chan.ui.view.bottom_menu_panel.BottomMenuPanel
import com.github.k1rakishou.chan.ui.view.bottom_menu_panel.BottomMenuPanelItem

interface MainControllerCallbacks {
  val isBottomPanelShown: Boolean
  val bottomPanelHeight: Int

  fun passMotionEventIntoDrawer(event: MotionEvent): Boolean
  fun onBottomPanelStateChanged(func: (BottomMenuPanel.State) -> Unit)

  fun showBottomPanel(controllerKey: ControllerKey, items: List<BottomMenuPanelItem>)
  fun hideBottomPanel(controllerKey: ControllerKey)
  fun passOnBackToBottomPanel(controllerKey: ControllerKey): Boolean
}