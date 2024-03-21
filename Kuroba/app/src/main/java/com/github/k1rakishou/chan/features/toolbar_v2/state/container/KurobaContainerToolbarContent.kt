package com.github.k1rakishou.chan.features.toolbar_v2.state.container

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.github.k1rakishou.chan.R
import com.github.k1rakishou.chan.features.toolbar_v2.KurobaToolbarState
import com.github.k1rakishou.chan.features.toolbar_v2.state.IKurobaToolbarState
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

  Column(
    modifier = Modifier
      .height(totalToolbarHeight)
      .background(chanTheme.backColorSecondaryCompose),
  ) {
    Spacer(modifier = Modifier.height(windowInsets.top))

    AnimatedContent(
      modifier = Modifier.fillMaxSize(),
      targetState = topToolbarState,
      transitionSpec = {
        fadeIn(animationSpec = tween(220, delayMillis = 90))
          .togetherWith(fadeOut(animationSpec = tween(90)))
      },
      label = "Toolbar state change animation"
    ) { topToolbarState ->
      childToolbar(topToolbarState)
    }
  }

}