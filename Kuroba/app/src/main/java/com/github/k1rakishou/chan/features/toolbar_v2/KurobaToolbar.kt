package com.github.k1rakishou.chan.features.toolbar_v2

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.github.k1rakishou.chan.R
import com.github.k1rakishou.chan.ui.compose.components.KurobaComposeClickableIcon
import com.github.k1rakishou.chan.ui.compose.components.KurobaComposeText
import com.github.k1rakishou.chan.ui.compose.ktu
import com.github.k1rakishou.chan.ui.compose.providers.LocalChanTheme
import com.github.k1rakishou.chan.ui.compose.providers.LocalWindowInsets

@Composable
fun KurobaToolbar(
  kurobaToolbarState: KurobaToolbarState?
) {
  val chanTheme = LocalChanTheme.current
  val windowInsets = LocalWindowInsets.current
  val toolbarHeight = dimensionResource(id = R.dimen.toolbar_height)
  val totalToolbarHeight = windowInsets.top + toolbarHeight

  LaunchedEffect(totalToolbarHeight) {
    kurobaToolbarState?.onToolbarHeightChanged(totalToolbarHeight)
  }

  if (kurobaToolbarState == null) {
    return
  }

  val enabled by kurobaToolbarState.enabledState
  if (!enabled) {
    return
  }

  val toolbarLayer = kurobaToolbarState.topLayer

  Column(
    modifier = Modifier
      .height(totalToolbarHeight)
      .background(chanTheme.backColorSecondaryCompose),
  ) {
    Spacer(modifier = Modifier.height(windowInsets.top))

    AnimatedContent(
      targetState = toolbarLayer,
      transitionSpec = {
        fadeIn(animationSpec = tween(220, delayMillis = 90))
          .togetherWith(fadeOut(animationSpec = tween(90)))
      },
      label = "Toolbar state change animation"
    ) { targetState ->
      val leftIcon = targetState.leftIcon
      val middleContent = targetState.middleContent
      val rightIcons = targetState.toolbarMenu.menuItems

      Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (leftIcon != null) {
          Spacer(modifier = Modifier.width(8.dp))

          val iconDrawableId by leftIcon.drawableIdState
          KurobaComposeClickableIcon(
            drawableId = iconDrawableId,
            onClick = {
              if (toolbarLayer.iconClickInterceptor(leftIcon)) {
                leftIcon.onClick(leftIcon)
              }
            }
          )

          Spacer(modifier = Modifier.width(12.dp))
        }

        when (middleContent) {
          is ToolbarMiddleContent.Custom -> middleContent.content()
          is ToolbarMiddleContent.Title -> {
            if (middleContent.subtitle == null) {
              if (middleContent.title != null) {
                ToolbarTitle(title = middleContent.title)
              }
            } else {
              if (middleContent.title != null) {
                ToolbarTitleWithSubtitle(
                  title = middleContent.title,
                  subtitle = middleContent.subtitle
                )
              }
            }
          }
          null -> {
            // no-op
          }
        }

        Spacer(modifier = Modifier.weight(1f))

        Spacer(modifier = Modifier.width(12.dp))

        for ((index, rightIcon) in rightIcons.withIndex()) {
          val iconDrawableId by rightIcon.drawableIdState
          val visible by rightIcon.visibleState

          if (!visible) {
            continue
          }

          KurobaComposeClickableIcon(
            drawableId = iconDrawableId,
            onClick = {
              if (toolbarLayer.iconClickInterceptor(rightIcon)) {
                rightIcon.onClick(rightIcon)
              }
            }
          )

          if (index != rightIcons.lastIndex) {
            Spacer(modifier = Modifier.width(12.dp))
          } else {
            Spacer(modifier = Modifier.width(8.dp))
          }
        }
      }
    }
  }
}

@Composable
private fun ToolbarTitle(title: ToolbarText) {
  KurobaComposeText(
    text = title.resolve(),
    fontSize = 18.ktu
  )
}

@Composable
private fun ToolbarTitleWithSubtitle(title: ToolbarText, subtitle: ToolbarText) {
  Column(modifier = Modifier.fillMaxSize()) {
    KurobaComposeText(
      modifier = Modifier.weight(0.7f),
      text = title.resolve(),
      fontSize = 16.ktu
    )

    KurobaComposeText(
      modifier = Modifier.weight(0.3f),
      text = subtitle.resolve(),
      fontSize = 14.ktu
    )
  }
}