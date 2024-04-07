package com.github.k1rakishou.chan.features.toolbar_v2.state.container.transition

import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.util.lerp
import com.github.k1rakishou.chan.ui.controller.base.transition.TransitionMode

internal fun GraphicsLayerScope.animateTransitionProgress(
  transitionMode: TransitionMode,
  progress: Float,
  toolbarHeightPx: Float,
  isOldToolbar: Boolean
) {
  if (progress < 0f) {
    alpha = 0f
    return
  }

  fun GraphicsLayerScope.animateToolbarOut(
    toolbarHeightPx: Float,
    progress: Float
  ) {
    alpha = 1f - progress
    translationY = lerp(0f, -(toolbarHeightPx / 2f), progress)
  }

  fun GraphicsLayerScope.animateToolbarIn(
    toolbarHeightPx: Float,
    progress: Float
  ) {
    alpha = progress
    translationY = lerp((toolbarHeightPx / 2f), 0f, progress)
  }

  when (transitionMode) {
    TransitionMode.In -> {
      if (isOldToolbar) {
        animateToolbarOut(
          toolbarHeightPx = toolbarHeightPx,
          progress = progress
        )
      } else {
        animateToolbarIn(
          toolbarHeightPx = toolbarHeightPx,
          progress = progress
        )
      }
    }
    TransitionMode.Out -> {
      if (isOldToolbar) {
        animateToolbarIn(
          toolbarHeightPx = toolbarHeightPx,
          progress = progress
        )
      } else {
        animateToolbarOut(
          toolbarHeightPx = toolbarHeightPx,
          progress = progress
        )
      }
    }
  }
}