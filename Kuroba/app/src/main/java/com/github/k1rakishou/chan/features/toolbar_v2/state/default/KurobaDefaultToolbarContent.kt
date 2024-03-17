package com.github.k1rakishou.chan.features.toolbar_v2.state.default

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.k1rakishou.chan.features.toolbar_v2.ToolbarMiddleContent
import com.github.k1rakishou.chan.features.toolbar_v2.ToolbarText
import com.github.k1rakishou.chan.ui.compose.components.KurobaComposeClickableIcon
import com.github.k1rakishou.chan.ui.compose.components.KurobaComposeText
import com.github.k1rakishou.chan.ui.compose.ktu

@Composable
fun KurobaDefaultToolbarContent(state: KurobaDefaultToolbarState) {
  val leftIconMut by state.leftItem
  val leftIcon = leftIconMut

  val middleContentMut by state.middleContent
  val middleContent = middleContentMut

  val toolbarMenuMut by state.toolbarMenu
  val toolbarMenu = toolbarMenuMut

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
          val iconClickInterceptor = state.iconClickInterceptor

          if (iconClickInterceptor == null || iconClickInterceptor(leftIcon)) {
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

    if (toolbarMenu != null) {
      val menuItems = toolbarMenu.menuItems
      if (menuItems.isNotEmpty()) {
        Spacer(modifier = Modifier.width(12.dp))

        for ((index, rightIcon) in menuItems.withIndex()) {
          val iconDrawableId by rightIcon.drawableIdState
          val visible by rightIcon.visibleState

          if (!visible) {
            continue
          }

          KurobaComposeClickableIcon(
            drawableId = iconDrawableId,
            onClick = {
              val iconClickInterceptor = state.iconClickInterceptor

              if (iconClickInterceptor == null || iconClickInterceptor(rightIcon)) {
                rightIcon.onClick(rightIcon)
              }
            }
          )

          if (index != menuItems.lastIndex) {
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