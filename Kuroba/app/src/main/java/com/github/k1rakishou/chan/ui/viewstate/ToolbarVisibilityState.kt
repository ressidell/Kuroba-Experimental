package com.github.k1rakishou.chan.ui.viewstate

import com.github.k1rakishou.ChanSettings
import com.github.k1rakishou.chan.core.manager.CurrentFocusedController
import com.github.k1rakishou.chan.features.toolbar.state.ToolbarStateKind
import com.github.k1rakishou.chan.ui.controller.BrowseController
import com.github.k1rakishou.chan.ui.controller.ViewThreadController
import com.github.k1rakishou.chan.ui.controller.base.ControllerKey
import kotlinx.collections.immutable.ImmutableMap

data class ToolbarVisibilityState(
  val replyLayoutVisibilityStates: ReplyLayoutVisibilityStates,
  val isDraggingFastScroller: Boolean,
  val scrollProgress: Float,
  val currentToolbarStates: ImmutableMap<ControllerKey, ToolbarStateKind>,
  val currentFocusedController: CurrentFocusedController,
  val topControllerKeys: List<ControllerKey>
) {
  private val catalogScreenKey
    get() = BrowseController.catalogControllerKey
  private val threadScreenKey
    get() = ViewThreadController.threadControllerKey

  val toolbarAlpha: Float
    get() = scrollProgress

  fun isToolbarForceVisible(): Boolean {
    if (topControllerKeys.isEmpty()) {
      return false
    }

    if (isDraggingFastScroller) {
      return true
    }

    if (replyLayoutVisibilityStates.anyOpenedOrExpanded()) {
      return true
    }

    if (!ChanSettings.canCollapseToolbar()) {
      return true
    }

    if (!ChanSettings.isSplitLayoutMode() && (topControllerKeys.any { key -> key == catalogScreenKey || key == threadScreenKey } )) {
      val currentFocusedScreenKey = when (currentFocusedController) {
        CurrentFocusedController.Catalog -> catalogScreenKey
        CurrentFocusedController.Thread -> threadScreenKey
        CurrentFocusedController.None -> null
      }

      if (currentFocusedScreenKey != null && currentToolbarStates[currentFocusedScreenKey]?.needForceShowToolbar() == true) {
        return true
      }

      // fallthrough
    } else if (currentToolbarStates[topControllerKeys.first()]?.needForceShowToolbar() == true) {
      return true
    }

    return false
  }

  fun isToolbarForceHidden(): Boolean {
    if (topControllerKeys.isEmpty()) {
      return true
    }

    return false
  }

}