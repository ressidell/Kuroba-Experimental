package com.github.k1rakishou.chan.features.toolbar_v2.state.container.transition

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.github.k1rakishou.chan.controller.transition.TransitionMode
import com.github.k1rakishou.chan.features.toolbar_v2.KurobaToolbarTransition
import com.github.k1rakishou.chan.features.toolbar_v2.state.IKurobaToolbarState
import com.github.k1rakishou.chan.ui.compose.consumeClicks

@Composable
internal fun KurobaToolbarTransitionInstant(
  toolbarHeight: Dp,
  transitionToolbarState: KurobaToolbarTransition,
  topToolbarState: IKurobaToolbarState?,
  childToolbarMovable: @Composable (IKurobaToolbarState?) -> Unit,
  childToolbar: @Composable (IKurobaToolbarState?) -> Unit,
  onAnimationFinished: () -> Unit
) {
  transitionToolbarState as KurobaToolbarTransition.Instant

  val toolbarHeightPx = with(LocalDensity.current) { toolbarHeight.toPx() }
  val progressAnimatable = remember { Animatable(initialValue = -1f) }

  LaunchedEffect(key1 = transitionToolbarState) {
    when (transitionToolbarState.transitionMode) {
      TransitionMode.In -> {
        try {
          progressAnimatable.snapTo(0f)
          progressAnimatable.animateTo(1f)
        } finally {
          onAnimationFinished()
          progressAnimatable.snapTo(1f)
        }
      }
      TransitionMode.Out -> {
        try {
          progressAnimatable.snapTo(1f)
          progressAnimatable.animateTo(0f)
        } finally {
          onAnimationFinished()
          progressAnimatable.snapTo(0f)
        }
      }
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .consumeClicks(enabled = true)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .graphicsLayer {
          animateTransitionProgress(
            transitionMode = transitionToolbarState.transitionMode,
            progress = progressAnimatable.value,
            toolbarHeightPx = toolbarHeightPx,
            isOldToolbar = true
          )
        }
    ) {
      childToolbarMovable(topToolbarState)
    }

    Box(
      modifier = Modifier
        .fillMaxSize()
        .graphicsLayer {
          animateTransitionProgress(
            transitionMode = transitionToolbarState.transitionMode,
            progress = progressAnimatable.value,
            toolbarHeightPx = toolbarHeightPx,
            isOldToolbar = false
          )
        }
    ) {
      childToolbar(transitionToolbarState.transitionToolbarState)
    }
  }
}