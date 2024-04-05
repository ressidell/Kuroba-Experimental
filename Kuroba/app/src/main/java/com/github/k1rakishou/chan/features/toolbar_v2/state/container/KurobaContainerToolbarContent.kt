package com.github.k1rakishou.chan.features.toolbar_v2.state.container

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.github.k1rakishou.chan.R
import com.github.k1rakishou.chan.features.toolbar_v2.KurobaToolbarState
import com.github.k1rakishou.chan.features.toolbar_v2.KurobaToolbarTransition
import com.github.k1rakishou.chan.features.toolbar_v2.state.KurobaToolbarSubState
import com.github.k1rakishou.chan.features.toolbar_v2.state.container.transition.KurobaToolbarTransitionInstant
import com.github.k1rakishou.chan.features.toolbar_v2.state.container.transition.KurobaToolbarTransitionProgress
import com.github.k1rakishou.chan.ui.compose.providers.LocalChanTheme
import com.github.k1rakishou.chan.ui.compose.providers.LocalWindowInsets

@Composable
fun KurobaContainerToolbarContent(
  kurobaToolbarState: KurobaToolbarState,
  childToolbar: @Composable (KurobaToolbarSubState?) -> Unit
) {
  val chanTheme = LocalChanTheme.current
  val windowInsets = LocalWindowInsets.current

  val toolbarHeight = dimensionResource(id = R.dimen.toolbar_height)
  val totalToolbarHeight = windowInsets.top + toolbarHeight

  LaunchedEffect(totalToolbarHeight) {
    kurobaToolbarState.onToolbarHeightChanged(totalToolbarHeight)
  }

  val toolbarStates by kurobaToolbarState.toolbarStateList
  val topToolbarState = toolbarStates.lastOrNull()

  if (topToolbarState is KurobaContainerToolbarSubState) {
    return
  }

  val transitionToolbarMut by kurobaToolbarState.transitionToolbarState
  val transitionToolbar = transitionToolbarMut

  val childToolbarMovable = remember {
    movableContentOf { topToolbarState: KurobaToolbarSubState? ->
      childToolbar(topToolbarState)
    }
  }

  Column(
    modifier = Modifier
      .height(totalToolbarHeight)
      .background(chanTheme.toolbarBackgroundComposeColor),
  ) {
    Spacer(modifier = Modifier.height(windowInsets.top))

    when (transitionToolbar) {
      null,
      is KurobaToolbarTransition.Progress -> {
        KurobaToolbarTransitionProgress(
          toolbarHeight = toolbarHeight,
          transitionToolbarState = transitionToolbar,
          topToolbarState = topToolbarState,
          childToolbarMovable = childToolbarMovable,
          childToolbar = childToolbar
        )
      }
      is KurobaToolbarTransition.Instant -> {
        KurobaToolbarTransitionInstant(
          toolbarHeight = toolbarHeight,
          transitionToolbarState = transitionToolbar,
          topToolbarState = topToolbarState,
          childToolbarMovable = childToolbarMovable,
          childToolbar = childToolbar,
          onAnimationFinished = { kurobaToolbarState.onKurobaToolbarTransitionInstantFinished(transitionToolbar) }
        )
      }
    }
  }

}