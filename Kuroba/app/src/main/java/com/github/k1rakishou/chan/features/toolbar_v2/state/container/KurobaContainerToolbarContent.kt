package com.github.k1rakishou.chan.features.toolbar_v2.state.container

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.lerp
import com.github.k1rakishou.chan.R
import com.github.k1rakishou.chan.controller.transition.TransitionMode
import com.github.k1rakishou.chan.features.toolbar_v2.KurobaToolbarState
import com.github.k1rakishou.chan.features.toolbar_v2.KurobaToolbarTransition
import com.github.k1rakishou.chan.features.toolbar_v2.state.IKurobaToolbarState
import com.github.k1rakishou.chan.ui.compose.consumeClicks
import com.github.k1rakishou.chan.ui.compose.providers.LocalChanTheme
import com.github.k1rakishou.chan.ui.compose.providers.LocalWindowInsets

@Composable
fun KurobaContainerToolbarContent(
  kurobaToolbarState: KurobaToolbarState,
  childToolbar: @Composable (IKurobaToolbarState?) -> Unit
) {
  val chanTheme = LocalChanTheme.current
  val windowInsets = LocalWindowInsets.current

  val toolbarHeight = dimensionResource(id = R.dimen.toolbar_height)
  val totalToolbarHeight = windowInsets.top + toolbarHeight

  LaunchedEffect(totalToolbarHeight) {
    kurobaToolbarState.onToolbarHeightChanged(totalToolbarHeight)
  }

  val topToolbarState = kurobaToolbarState.topToolbar
  if (topToolbarState is KurobaContainerToolbarState) {
    return
  }

  val transitionToolbarMut by kurobaToolbarState.transitionToolbarState
  val transitionToolbar = transitionToolbarMut

  val childToolbarMovable = remember {
    movableContentOf { topToolbarState: IKurobaToolbarState? ->
      childToolbar(topToolbarState)
    }
  }

  Column(
    modifier = Modifier
      .height(totalToolbarHeight)
      .background(chanTheme.toolbarBackgroundComposeColor),
  ) {
    Spacer(modifier = Modifier.height(windowInsets.top))

    if (transitionToolbar == null) {
      childToolbarMovable(topToolbarState)
    } else {
      ToolbarTransitionContainer(
        toolbarHeight = toolbarHeight,
        transitionToolbarState = transitionToolbar,
        topToolbarState = topToolbarState,
        childToolbarMovable = childToolbarMovable,
        childToolbar = childToolbar
      )
    }
  }

}

@Composable
private fun ToolbarTransitionContainer(
  toolbarHeight: Dp,
  transitionToolbarState: KurobaToolbarTransition,
  topToolbarState: IKurobaToolbarState?,
  childToolbarMovable: @Composable (IKurobaToolbarState?) -> Unit,
  childToolbar: @Composable (IKurobaToolbarState?) -> Unit
) {
  val toolbarHeightPx = with(LocalDensity.current) { toolbarHeight.toPx() }

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
            transitionToolbarState = transitionToolbarState,
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
            transitionToolbarState = transitionToolbarState,
            toolbarHeightPx = toolbarHeightPx,
            isOldToolbar = false
          )
        }
    ) {
      childToolbar(transitionToolbarState.transitionToolbarState)
    }
  }
}

private fun GraphicsLayerScope.animateTransitionProgress(
  transitionToolbarState: KurobaToolbarTransition,
  toolbarHeightPx: Float,
  isOldToolbar: Boolean
) {

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

  when (transitionToolbarState.transitionMode) {
    TransitionMode.In -> {
      if (isOldToolbar) {
        animateToolbarIn(
          toolbarHeightPx = toolbarHeightPx,
          progress = transitionToolbarState.progress
        )
      } else {
        animateToolbarOut(
          toolbarHeightPx = toolbarHeightPx,
          progress = transitionToolbarState.progress
        )
      }
    }
    TransitionMode.Out -> {
      if (isOldToolbar) {
        animateToolbarOut(
          toolbarHeightPx = toolbarHeightPx,
          progress = transitionToolbarState.progress
        )
      } else {
        animateToolbarIn(
          toolbarHeightPx = toolbarHeightPx,
          progress = transitionToolbarState.progress
        )
      }
    }
  }
}