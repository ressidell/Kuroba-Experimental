package com.github.k1rakishou.chan.features.toolbar_v2

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.github.k1rakishou.chan.R
import com.github.k1rakishou.chan.features.toolbar_v2.state.default.KurobaDefaultToolbarContent
import com.github.k1rakishou.chan.features.toolbar_v2.state.default.KurobaDefaultToolbarState
import com.github.k1rakishou.chan.features.toolbar_v2.state.reply.KurobaReplyToolbarContent
import com.github.k1rakishou.chan.features.toolbar_v2.state.reply.KurobaReplyToolbarState
import com.github.k1rakishou.chan.features.toolbar_v2.state.search.KurobaSearchToolbarContent
import com.github.k1rakishou.chan.features.toolbar_v2.state.search.KurobaSearchToolbarState
import com.github.k1rakishou.chan.features.toolbar_v2.state.selection.KurobaSelectionToolbarContent
import com.github.k1rakishou.chan.features.toolbar_v2.state.selection.KurobaSelectionToolbarState
import com.github.k1rakishou.chan.ui.compose.providers.LocalChanTheme
import com.github.k1rakishou.chan.ui.compose.providers.LocalWindowInsets

@Composable
fun KurobaToolbar(
  kurobaToolbarState: KurobaToolbarState?
) {
  val chanTheme = LocalChanTheme.current
  val windowInsets = LocalWindowInsets.current

  if (kurobaToolbarState == null) {
    return
  }

  val enabled by kurobaToolbarState.enabledState
  if (!enabled) {
    return
  }

  val toolbarHeight = dimensionResource(id = R.dimen.toolbar_height)
  val totalToolbarHeight = windowInsets.top + toolbarHeight

  LaunchedEffect(totalToolbarHeight) {
    kurobaToolbarState.onToolbarHeightChanged(totalToolbarHeight)
  }

  val toolbarState = kurobaToolbarState.topToolbar

  Column(
    modifier = Modifier
      .height(totalToolbarHeight)
      .background(chanTheme.backColorSecondaryCompose),
  ) {
    Spacer(modifier = Modifier.height(windowInsets.top))

    AnimatedContent(
      targetState = toolbarState,
      transitionSpec = {
        fadeIn(animationSpec = tween(220, delayMillis = 90))
          .togetherWith(fadeOut(animationSpec = tween(90)))
      },
      label = "Toolbar state change animation"
    ) { topToolbarState ->
      when (topToolbarState) {
        is KurobaDefaultToolbarState -> {
          KurobaDefaultToolbarContent(topToolbarState)
        }
        is KurobaSearchToolbarState -> {
          KurobaSearchToolbarContent(topToolbarState)
        }
        is KurobaSelectionToolbarState -> {
          KurobaSelectionToolbarContent(topToolbarState)
        }
        is KurobaReplyToolbarState -> {
          KurobaReplyToolbarContent(topToolbarState)
        }
      }
    }
  }
}