package com.github.k1rakishou.chan.ui.viewstate

import com.github.k1rakishou.ChanSettings
import com.github.k1rakishou.chan.core.manager.CurrentFocusedController
import com.github.k1rakishou.chan.features.toolbar.state.ToolbarStateKind
import com.github.k1rakishou.chan.ui.controller.BrowseController
import com.github.k1rakishou.chan.ui.controller.ThreadControllerType
import com.github.k1rakishou.chan.ui.controller.ViewThreadController
import com.github.k1rakishou.chan.ui.controller.base.ControllerKey
import com.github.k1rakishou.chan.ui.layout.ThreadLayout
import kotlinx.collections.immutable.ImmutableMap

data class FabVisibilityState(
  val fabEnabled: Boolean,
  val replyLayoutVisibilityStates: ReplyLayoutVisibilityStates,
  val threadLayoutState: ThreadLayout.State,
  val focusedController: ThreadControllerType,
  val scrollProgress: Float,
  val isDraggingFastScroller: Boolean,
  val snackbarVisible: Boolean,
  val currentToolbarStates: ImmutableMap<ControllerKey, ToolbarStateKind>,
  val currentFocusedController: CurrentFocusedController
) {
  private val catalogScreenKey
    get() = BrowseController.catalogControllerKey
  private val threadScreenKey
    get() = ViewThreadController.threadControllerKey

  val fabAlpha: Float
    get() = scrollProgress

  fun isFabForceVisible(threadControllerType: ThreadControllerType, controllerKey: ControllerKey): Boolean {
    if (!fabEnabled) {
      return false
    }

    if (ChanSettings.isSplitLayoutMode()) {
      if (isCurrentReplyLayoutOpened(threadControllerType)) {
        return false
      }

      if (threadLayoutState.isNotInContentState()) {
        return false
      }

      // fallthrough
    }

    if (isForceHiddenBasedOnCurrentFocusedController(controllerKey)) {
      // Can't be visible if it's force hidden
      return false
    }

    return !ChanSettings.canCollapseToolbar()
  }

  fun isFabForceHidden(threadControllerType: ThreadControllerType, controllerKey: ControllerKey): Boolean {
    if (!fabEnabled) {
      return true
    }

    if (ChanSettings.isSplitLayoutMode()) {
      if (isCurrentReplyLayoutOpened(threadControllerType)) {
        return true
      }

      if (threadLayoutState.isNotInContentState()) {
        return true
      }

      return false
    }

    if (isForceHiddenBasedOnCurrentFocusedController(controllerKey)) {
      return true
    }

    if (threadLayoutState.isNotInContentState()) {
      return true
    }

    if (focusedController != threadControllerType) {
      return true
    }

    if (isDraggingFastScroller) {
      return true
    }

    if (isCurrentReplyLayoutOpened(threadControllerType)) {
      return true
    }

    if (snackbarVisible) {
      return true
    }

    return false
  }

  private fun isCurrentReplyLayoutOpened(threadControllerType: ThreadControllerType): Boolean {
    return when (threadControllerType) {
      ThreadControllerType.Catalog -> replyLayoutVisibilityStates.catalog.isOpenedOrExpanded()
      ThreadControllerType.Thread -> replyLayoutVisibilityStates.thread.isOpenedOrExpanded()
    }
  }

  private fun isForceHiddenBasedOnCurrentFocusedController(controllerKey: ControllerKey): Boolean {
    if (!ChanSettings.isSplitLayoutMode() && (controllerKey == catalogScreenKey || controllerKey == threadScreenKey)) {
      val currentFocusedScreenKey = when (currentFocusedController) {
        CurrentFocusedController.Catalog -> catalogScreenKey
        CurrentFocusedController.Thread -> threadScreenKey
        CurrentFocusedController.None -> null
      }

      if (currentFocusedScreenKey != null && currentToolbarStates[currentFocusedScreenKey]?.needForceHideFab() == true) {
        return true
      }

      // fallthrough
    } else if (currentToolbarStates[controllerKey]?.needForceHideFab() == true) {
      return true
    }

    return false
  }

}